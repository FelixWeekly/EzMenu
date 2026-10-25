package com.ezmenu.app.ui.screens.cooking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ezmenu.app.EzMenuApplication
import com.ezmenu.app.data.repository.RecipeRepository
import com.ezmenu.app.data.repository.SettingsRepository
import com.ezmenu.app.domain.model.RecipeDetail
import com.ezmenu.app.domain.model.StepView
import com.ezmenu.app.domain.cookingStepIndex
import com.ezmenu.app.domain.initialServings
import kotlinx.coroutines.CancellationException
import com.ezmenu.app.navigation.Routes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CookingUiState(
    val detail: RecipeDetail? = null,
    val stepIndex: Int = 0,
    /** 当前步骤的剩余秒数；`null` 表示这一步没有计时 */
    val remainingSeconds: Int? = null,
    val timerTotalSeconds: Int? = null,
    val timerRunning: Boolean = false,
    val keepScreenOn: Boolean = true,
    val chefTipVisible: Boolean = false,
    val toast: String? = null,
    val finished: Boolean = false,
    val loading: Boolean = true,
    val servings: Int = 2,
    val completing: Boolean = false,
    val error: String? = null,
) {
    val currentStep: StepView? get() = detail?.steps?.getOrNull(stepIndex)
    val stepCount: Int get() = detail?.steps?.size ?: 0
    val isFirstStep: Boolean get() = stepIndex <= 0
    val isLastStep: Boolean get() = stepIndex >= stepCount - 1

    /** 倒计时进度 0f..1f，用于进度条。 */
    val progress: Float
        get() {
            val total = timerTotalSeconds ?: return 0f
            val remaining = remainingSeconds ?: return 0f
            if (total <= 0) return 0f
            return (remaining.toFloat() / total).coerceIn(0f, 1f)
        }
}

/**
 * 全屏烹饪引导。
 *
 * 设计稿这一屏的计时器**完全是死的** —— `02:36` 和 78% 的进度条写死在 HTML 里，
 * 没有任何 `setInterval`；「30秒」「重置」「上一步」「下一步」「聆听」五个按钮
 * 也都没有绑定事件。这里全部接上真实逻辑。
 *
 * 手势与语音仍只做视觉提示，不接入摄像头与麦克风 —— 那需要权限与相机管线，
 * 且手势识别本身是个独立的算法问题，不属于本次还原设计的范围。
 */
class CookingViewModel(
    private val repository: RecipeRepository,
    private val settings: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val recipeId: Long = savedStateHandle[Routes.ARG_RECIPE_ID] ?: 0L
    private val startStep: Int = savedStateHandle[Routes.ARG_START_STEP] ?: 0

    private val _uiState = MutableStateFlow(CookingUiState())
    val uiState: StateFlow<CookingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val n = initialServings(savedStateHandle.get<Int>(Routes.ARG_SERVINGS)?.takeIf { it > 0 }, settings.defaultServings.first())
                val detail = repository.getDetailOnce(recipeId, targetServings = n)
                val index = cookingStepIndex(startStep, detail?.steps?.size ?: 0)
                val keepOn = settings.keepScreenOn.first()
                _uiState.update {
                    it.copy(detail = detail, stepIndex = index ?: 0, servings = n,
                        keepScreenOn = keepOn, loading = false,
                        error = when { detail == null -> "菜谱不存在"; index == null -> "没有可引导的步骤"; else -> null })
                }
                resetTimerForCurrentStep()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = "菜谱加载失败，请返回后重试") }
            }
        }
    }

    // ------------------------------------------------------------ 步骤切换

    fun onPreviousStep() {
        if (_uiState.value.isFirstStep || _uiState.value.completing || _uiState.value.finished) return
        _uiState.update { it.copy(stepIndex = it.stepIndex - 1) }
        resetTimerForCurrentStep()
    }

    fun onNextStep() {
        val state = _uiState.value
        if (state.loading || state.error != null || state.currentStep == null || state.completing || state.finished) return
        if (state.isLastStep) {
            completeCooking()
            return
        }
        _uiState.update { it.copy(stepIndex = it.stepIndex + 1) }
        resetTimerForCurrentStep()
    }

    /** 走完全部步骤：记一次下厨，然后退出。 */
    private fun completeCooking() {
        if (_uiState.value.completing || _uiState.value.finished) return
        stopTimer()
        _uiState.update { it.copy(completing = true) }
        viewModelScope.launch {
            try {
                repository.recordCook(recipeId, _uiState.value.servings)
                _uiState.update { it.copy(completing = false, finished = true, toast = "🎉 完成！已记入下厨记录") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(completing = false, toast = "记录保存失败，请重试") }
            }
        }
    }

    // ------------------------------------------------------------ 计时器

    /**
     * 切换当前步骤的计时起停。
     *
     * 剩余为 0 时先重置再启动，这样「重置」和「开始」共用一个按钮逻辑，
     * 与设计稿里图标在 `play_arrow / pause / replay` 之间切换的表现一致。
     */
    fun onToggleTimer() {
        val state = _uiState.value
        val remaining = state.remainingSeconds ?: return

        if (state.timerRunning) {
            stopTimer()
            return
        }
        if (remaining <= 0) {
            resetTimerForCurrentStep()
            startTimer()
        } else {
            startTimer()
        }
    }

    /** 「30秒」——在实际做菜场景里，加时比减时有用得多。 */
    fun onAddThirtySeconds() {
        _uiState.update { state ->
            val remaining = state.remainingSeconds ?: return@update state
            val total = state.timerTotalSeconds ?: return@update state
            state.copy(
                remainingSeconds = remaining + 30,
                // 加时后总时长要跟着长，否则进度条会超过 100%
                timerTotalSeconds = total + 30,
            )
        }
    }

    fun onResetTimer() {
        stopTimer()
        _uiState.update {
            // 重置回「加时后的总时长」，而不是原始时长 —— 否则刚加的 30 秒会凭空消失
            it.copy(remainingSeconds = it.timerTotalSeconds)
        }
    }

    private fun resetTimerForCurrentStep() {
        stopTimer()
        val seconds = _uiState.value.currentStep?.timerSeconds
        _uiState.update {
            it.copy(
                remainingSeconds = seconds,
                timerTotalSeconds = seconds,
                timerRunning = false,
            )
        }
    }

    private fun startTimer() {
        stopTimer()
        _uiState.update { it.copy(timerRunning = true) }
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val current = _uiState.value.remainingSeconds ?: break
                if (current <= 0) break
                val next = current - 1
                _uiState.update {
                    it.copy(
                        remainingSeconds = next,
                        timerRunning = next > 0,
                        toast = if (next == 0) "叮！该翻面了" else it.toast,
                    )
                }
                if (next <= 0) break
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(timerRunning = false) }
    }

    // ------------------------------------------------------------ 弹层与提示

    fun onShowChefTip() = _uiState.update { it.copy(chefTipVisible = true) }

    fun onHideChefTip() = _uiState.update { it.copy(chefTipVisible = false) }

    fun onToastShown() = _uiState.update { it.copy(toast = null) }

    /** 手势/语音按钮：目前只给一次视觉反馈，不接摄像头与麦克风。 */
    fun onVoiceHint() = _uiState.update { it.copy(toast = "语音与手势控制尚未接入") }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as EzMenuApplication
                CookingViewModel(
                    app.services.recipeRepository,
                    app.services.settingsRepository,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
