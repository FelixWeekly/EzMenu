package com.ezmenu.app.ui.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ezmenu.app.EzMenuApplication
import com.ezmenu.app.data.local.dao.TagTriple
import com.ezmenu.app.data.local.entity.IngredientEntity
import com.ezmenu.app.data.local.entity.IngredientGroupEntity
import com.ezmenu.app.data.local.entity.RecipeEntity
import com.ezmenu.app.data.local.entity.StepEntity
import com.ezmenu.app.data.local.entity.TagKind
import com.ezmenu.app.data.repository.RecipeRepository
import com.ezmenu.app.data.repository.SettingsRepository
import com.ezmenu.app.data.local.relation.RecipeWithDetails
import com.ezmenu.app.domain.SavedRecipeResult
import com.ezmenu.app.domain.ServingsRules
import com.ezmenu.app.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.Job
import com.ezmenu.app.data.local.entity.EditorDraftEntity

/**
 * 编辑器里的食材行。
 *
 * [uid] 是**稳定标识**，不是列表下标 —— 增删行时若用下标做 key，
 * Compose 会复用错误的输入框状态，出现「删了第三行结果第二行的文字变了」这类问题。
 */
data class EditableIngredient(
    val uid: Long,
    val name: String = "",
    val amountText: String = "",
    val unit: String = "克",
    val isRawAmount: Boolean = false,
    val groupId: Long = 0,
    val original: IngredientEntity? = null,
)

data class EditableStep(
    val uid: Long,
    val title: String = "",
    val body: String = "",
    val timerSeconds: Int? = null,
    val noteText: String = "",
    val imageRef: String? = null,
    val original: StepEntity? = null,
)

data class EditorUiState(
    val recipeId: Long? = null,
    val title: String = "",
    val story: String = "",
    val coverRef: String? = null,
    val cookMinutes: Int = 30,
    val servings: Int = 2,
    val ingredients: List<EditableIngredient> = emptyList(),
    val steps: List<EditableStep> = emptyList(),
    val batchSheetVisible: Boolean = false,
    val batchText: String = "",
    val toast: String? = null,
    val savedAndExit: Boolean = false,
    val savedResult: SavedRecipeResult? = null,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val isPublished: Boolean = false,
    val loading: Boolean = true,
    val groups: List<IngredientGroupEntity> = listOf(IngredientGroupEntity(id=-1,recipeId=0,name="备料清单")),
    val category: String = "家常菜",
    val tags: List<String> = emptyList(),
    val autosavedAt: Long = 0,
) {
    val canDecrementServings: Boolean
        get() = ServingsRules.canDecrement(servings, ServingsRules.EDITOR_RANGE)
    val canIncrementServings: Boolean
        get() = ServingsRules.canIncrement(servings, ServingsRules.EDITOR_RANGE)

    /**
     * 计数文案统一成「N 味食材」「N 个步骤」。
     *
     * 设计稿这里有两套格式互相打架：静态 DOM 写的是「5 味食材」，而增删一行后
     * JS 会把它改成「(共 N 味)」。同一处文案两种写法，取前者。
     */
    val ingredientCountLabel: String get() = "${ingredients.size} 味食材"
    val stepCountLabel: String get() = "${steps.size} 个步骤"
}

/**
 * 编辑 / 新建菜谱。
 *
 * 设计稿这一屏的断链最多，实现时逐条补全：
 *   1. 「智能导入」是**空实现** —— 只弹个 toast，既不解析也不加行。这里真的解析。
 *   2. 静态行**没有**单位切换与复制按钮，而 JS 新建出来的行有 —— 两套模板不一致。
 *      这里统一成一套行组件。
 *   3. 计数器文案两套格式（见 [EditorUiState.ingredientCountLabel]）。
 *   4. 时间 pill 点击后形状会变（类名里写的是 `rounded` 而不是 `rounded-full`）—— 这里不会。
 *   5. 份量上下限、标题校验等按设计稿的语义补齐。
 */
class RecipeEditorViewModel(
    private val repository: RecipeRepository,
    savedStateHandle: SavedStateHandle,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val editingId: Long? = savedStateHandle.get<Long>(Routes.ARG_RECIPE_ID)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val draftKey = editingId?.let { "recipe:"+it } ?: "new"
    private var autosaveJob: Job? = null
    private var suppressAutosave = false
    private var saveCompleted = false
    private var uidCounter = 0L
    private var original: RecipeWithDetails? = null
    private fun nextUid(): Long = ++uidCounter

    init {
        viewModelScope.launch {
            try {
                if (editingId == null) {
                    val preferred = settings.defaultServings.first()
                    // 新建：给一组空行，省得用户面对空白页
                    _uiState.update {
                        it.copy(
                            ingredients = List(3) { EditableIngredient(uid = nextUid()) },
                            steps = listOf(EditableStep(uid = nextUid())),
                            servings = ServingsRules.clampEditor(preferred),
                            loading = false,
                        )
                    }
                } else {
                    loadExisting(editingId)
                }
                repository.journalDao?.draft(draftKey)?.let { draft ->
                    _uiState.value = EditorDraftCodec.decode(draft.payload,_uiState.value)
                    uidCounter = maxOf(uidCounter,(_uiState.value.ingredients.map{it.uid}+_uiState.value.steps.map{it.uid}+_uiState.value.groups.map{kotlin.math.abs(it.id)}).maxOrNull()?:0)
                }
                autosaveJob = viewModelScope.launch {
                    uiState.map { EditorDraftCodec.encode(it) }.distinctUntilChanged().drop(1).collect {
                        if(!suppressAutosave && !saveCompleted) flushDraft()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, loadError = "菜谱加载失败", toast = "菜谱加载失败，请返回后重试") }
            }
        }
    }

    private suspend fun loadExisting(id: Long) {
        val graph = repository.getForEditing(id)
        if (graph == null) {
            _uiState.update { it.copy(loading = false, loadError = "菜谱不存在", toast = "菜谱不存在") }
            return
        }
        original = graph
        val recipe = graph.recipe
        _uiState.update {
            it.copy(
                recipeId = id,
                groups = graph.groups.ifEmpty { listOf(IngredientGroupEntity(id=-1,recipeId=0,name="备料清单")) },
                category = recipe.category,
                tags = graph.orderedTags.map { it.name },
                title = recipe.title,
                story = recipe.story.ifBlank { recipe.subtitle },
                coverRef = recipe.coverImageRef,
                cookMinutes = recipe.cookTimeMinutes,
                servings = recipe.baseServings,
                isPublished = !recipe.isDraft,
                ingredients = graph.orderedGroups
                    .flatMap { (_, items) -> items }
                    .map { item ->
                        EditableIngredient(
                            uid = nextUid(),
                            name = item.name,
                            amountText = item.baseAmount?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }
                                ?: item.rawDisplay.orEmpty(),
                            unit = item.unitLabel,
                            isRawAmount = item.baseAmount == null,
                            groupId = item.groupId,
                            original = item,
                        )
                    },
                steps = graph.orderedSteps.map { step ->
                    EditableStep(
                        uid = nextUid(),
                        title = step.title,
                        body = step.body,
                        timerSeconds = step.timerSeconds,
                        noteText = step.noteText.orEmpty(),
                        imageRef = step.imageRef,
                        original = step,
                    )
                },
                loading = false,
            )
        }
    }

    suspend fun flushDraft() {
        if(suppressAutosave || saveCompleted || _uiState.value.loading || _uiState.value.loadError!=null) return
        val dao=repository.journalDao?:return
        val state=_uiState.value
        try { val now=System.currentTimeMillis();dao.saveDraft(EditorDraftEntity(draftKey,state.recipeId,EditorDraftCodec.encode(state),state.title,now));_uiState.update{it.copy(autosavedAt=now)} }
        catch(e:CancellationException){throw e} catch(e:Exception){_uiState.update{it.copy(toast="草稿暂存失败，请手动保存")}}
    }
    suspend fun discardDraft() { suppressAutosave=true;autosaveJob?.cancel();repository.journalDao?.deleteDraft(draftKey) }
    fun onApplyParsed(rows: List<EditableIngredient>) = _uiState.update { state ->
        state.copy(ingredients=state.ingredients.filter{it.name.isNotBlank()}+rows.map{it.copy(uid=nextUid())},batchSheetVisible=false,batchText="")
    }
    fun onCategoryChange(value: String)=_uiState.update{it.copy(category=value)}
    fun onTagsChange(value: String)=_uiState.update{it.copy(tags=value.split('，',',','、').map{n->n.trim()}.filter{n->n.isNotBlank()}.distinct())}
    fun onAddGroup(name: String)=_uiState.update{it.copy(groups=it.groups+IngredientGroupEntity(id=-nextUid()-100,recipeId=0,name=name.ifBlank{"食材分组"},sortOrder=it.groups.size))}
    fun onGroupName(id: Long,name: String)=_uiState.update{it.copy(groups=it.groups.map{g->if(g.id==id)g.copy(name=name)else g})}
    fun onAddIngredientToGroup(id: Long)=_uiState.update{it.copy(ingredients=it.ingredients+EditableIngredient(nextUid(),groupId=id))}
    fun onSetStepTimer(uid: Long,seconds: Int?)=onStepChange(uid){it.copy(timerSeconds=seconds?.coerceIn(1,86400))}
    // ------------------------------------------------------------ 基础字段

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

    fun onStoryChange(value: String) = _uiState.update { it.copy(story = value) }

    fun onCookMinutesChange(minutes: Int) = _uiState.update { it.copy(cookMinutes = minutes) }

    fun onServingsDelta(delta: Int) = _uiState.update {
        it.copy(servings = ServingsRules.clampEditor(it.servings + delta))
    }

    fun onCoverChange(ref: String?) = _uiState.update { it.copy(coverRef = ref) }

    fun onCoverImportFailed() = _uiState.update { it.copy(toast = "封面导入失败，请重新选择图片") }

    // ------------------------------------------------------------ 食材行

    fun onAddIngredient() = _uiState.update {
        it.copy(ingredients = it.ingredients + EditableIngredient(uid = nextUid()))
    }

    fun onRemoveIngredient(uid: Long) = _uiState.update { state ->
        state.copy(ingredients = state.ingredients.filterNot { it.uid == uid })
    }

    fun onIngredientChange(uid: Long, transform: (EditableIngredient) -> EditableIngredient) =
        _uiState.update { state ->
            state.copy(
                ingredients = state.ingredients.map { if (it.uid == uid) transform(it) else it },
            )
        }

    /** 单位快捷切换（设计稿里只有 JS 新建出来的行才有这个功能）。 */
    fun onIngredientUnitChange(uid: Long, unit: String) =
        onIngredientChange(uid) { it.copy(unit = unit, isRawAmount = false) }

    /** 在两组常见单位之间轮换，对应设计稿静态行里那个不可编辑的单位文案。 */
    fun onIngredientCycleUnit(uid: Long) = onIngredientChange(uid) { row ->
        val cycle = listOf("克", "片", "个", "ml", "枝", "适量")
        val next = cycle[(cycle.indexOf(row.unit).takeIf { it >= 0 }?.plus(1) ?: 0) % cycle.size]
        row.copy(unit = next, isRawAmount = next == "适量")
    }

    // ------------------------------------------------------------ 步骤

    fun onAddStep() = _uiState.update {
        it.copy(steps = it.steps + EditableStep(uid = nextUid()))
    }

    fun onRemoveStep(uid: Long) = _uiState.update { state ->
        state.copy(steps = state.steps.filterNot { it.uid == uid })
    }

    fun onStepChange(uid: Long, transform: (EditableStep) -> EditableStep) =
        _uiState.update { state ->
            state.copy(steps = state.steps.map { if (it.uid == uid) transform(it) else it })
        }

    fun onMoveStep(uid: Long, delta: Int) = _uiState.update { state ->
        val list = state.steps.toMutableList()
        val index = list.indexOfFirst { it.uid == uid }
        val target = index + delta
        if (index < 0 || target !in list.indices) return@update state
        list[index] = list[target].also { list[target] = list[index] }
        state.copy(steps = list)
    }

    fun onCloneStep(uid: Long) = _uiState.update { state ->
        val index = state.steps.indexOfFirst { it.uid == uid }
        if (index < 0) return@update state
        val copy = state.steps[index].copy(uid = nextUid())
        state.copy(steps = state.steps.toMutableList().apply { add(index + 1, copy) })
    }

    /** 在两组常见计时时长之间轮换：null → 3 分 → 5 分 → 10 分 → null。 */
    fun onCycleStepTimer(uid: Long) = onStepChange(uid) { step ->
        val cycle = listOf(null, 180, 300, 600)
        val current = step.timerSeconds
        val next = cycle[(cycle.indexOf(current).takeIf { it >= 0 }?.plus(1) ?: 0) % cycle.size]
        step.copy(timerSeconds = next)
    }

    // ------------------------------------------------------------ 批量导入

    fun onOpenBatchSheet() = _uiState.update { it.copy(batchSheetVisible = true) }

    fun onCloseBatchSheet() = _uiState.update { it.copy(batchSheetVisible = false) }

    fun onBatchTextChange(value: String) = _uiState.update { it.copy(batchText = value) }

    /** 「填入吐司样例」——文案逐字取自设计稿。 */
    fun onFillSample() = _uiState.update {
        it.copy(
            batchText = listOf(
                "厚切吐司 2片",
                "鲜牛奶 80ml",
                "有机鸡蛋 1颗",
                "黄油 15克",
                "肉桂粉 适量",
            ).joinToString("\n"),
        )
    }

    /**
     * 解析并录入。
     *
     * 设计稿这里是个**空壳**：只弹「✨ 智能识别成功！食材已更新」然后关掉弹层，
     * 一行都不会真的加进去。这里做真实解析，支持两种行式：
     *   「厚切吐司 2片」  → 名称 + 数量 + 单位
     *   「肉桂粉 适量」   → 名称 + 不可换算的描述
     * 解析不出数量与单位的行按「名称 + 适量」处理，而不是丢弃 ——
     * 用户粘贴的东西默默消失比解析得不准更让人困惑。
     */
    fun onApplyBatch() {
        val lines = _uiState.value.batchText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            _uiState.update { it.copy(toast = "请先输入或粘贴文本") }
            return
        }

        val parsed = lines.map { line -> parseIngredientLine(line, nextUid()) }
        _uiState.update { state ->
            // 已有空行就复用它，避免末尾堆一堆空输入框
            val blanks = state.ingredients.filter { it.name.isBlank() }
            val kept = state.ingredients.filterNot { it.name.isBlank() }
            val merged = kept + parsed + blanks.drop(parsed.size)
            state.copy(
                ingredients = merged,
                batchSheetVisible = false,
                batchText = "",
                toast = "已识别 ${parsed.size} 项食材",
            )
        }
    }

    // ------------------------------------------------------------ 保存

    fun onSaveDraft() = save(asDraft = !_uiState.value.isPublished)

    fun onPublish() = save(asDraft = false)

    private fun save(asDraft: Boolean) {
        val state = _uiState.value
        if (state.loading || state.loadError != null || state.isSaving || saveCompleted) return
        if (!asDraft && state.title.isBlank()) {
            _uiState.update { it.copy(toast = "请先填写菜谱名称") }
            return
        }
        if (!asDraft && state.steps.none { it.title.isNotBlank() || it.body.isNotBlank() }) {
            _uiState.update { it.copy(toast = "请至少填写一个烹饪步骤") }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                suppressAutosave=true
                val result = persist(state, asDraft)
                repository.journalDao?.deleteDraft(draftKey)
                saveCompleted = true
                _uiState.update {
                    it.copy(recipeId = result.recipeId, isSaving = false,
                        savedResult = result, savedAndExit = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                suppressAutosave=false
                _uiState.update { it.copy(isSaving = false, toast = "保存失败，请重试") }
            }
        }
    }

    fun onToastShown() = _uiState.update { it.copy(toast = null) }

    fun onExitHandled() = _uiState.update { it.copy(savedAndExit = false, savedResult = null) }

    private suspend fun persist(state: EditorUiState, asDraft: Boolean): SavedRecipeResult {
        val old = original?.recipe
        val groups = state.groups.ifEmpty { listOf(IngredientGroupEntity(recipeId=0,name="备料清单")) }
        val ingredients = groups.mapIndexed { index, group ->
            state.ingredients.filter { row ->
                row.name.isNotBlank() && (row.groupId == group.id || (row.groupId == 0L && index == 0))
            }.map { row ->
                val amount = if (row.isRawAmount) null else row.amountText.trim().toDoubleOrNull()
                    ?.takeIf { it.isFinite() && it >= 0 }
                (row.original ?: IngredientEntity(groupId = 0, recipeId = 0, name = row.name)).copy(
                    name = row.name,
                    baseAmount = amount,
                    unitLabel = if (amount == null) "" else row.unit,
                    gramsPerUnit = row.original?.gramsPerUnit?.takeIf { row.unit == row.original.unitLabel && amount != null },
                    rawDisplay = if (amount == null) row.amountText.ifBlank { "适量" } else null,
                )
            }
        }
        val steps = state.steps.filter { it.title.isNotBlank() || it.body.isNotBlank() }.map { step ->
            (step.original ?: StepEntity(recipeId = 0, title = "", body = "")).copy(
                title = step.title, body = step.body, imageRef = step.imageRef,
                timerSeconds = step.timerSeconds,
                timerLabel = step.original?.timerLabel?.takeIf { step.timerSeconds == step.original.timerSeconds },
                noteText = step.noteText.takeIf { it.isNotBlank() },
            )
        }
        val storyChanged = old == null || state.story != old.story.ifBlank { old.subtitle }
        val recipe = (old ?: RecipeEntity(title = state.title)).copy(
            id = state.recipeId ?: 0,
            title = state.title.ifBlank { "未命名菜谱" },
            subtitle = if (storyChanged) state.story else old?.subtitle.orEmpty(),
            story = if (storyChanged) state.story else old?.story.orEmpty(),
            coverImageRef = state.coverRef, cookTimeMinutes = state.cookMinutes,
            baseServings = state.servings, isDraft = asDraft, category = state.category,
        )
        return repository.saveEdited(recipe, groups, ingredients, steps,
            state.tags.map { name -> TagTriple(name,original?.orderedTags?.firstOrNull{it.name==name}?.let{TagKind.valueOf(it.kind)}?:TagKind.SCENE) },editMetadata=true)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as EzMenuApplication
                RecipeEditorViewModel(app.services.recipeRepository, createSavedStateHandle(), app.services.settingsRepository)
            }
        }

        private val UNIT_WORDS = setOf(
            "g", "ml", "克", "千克", "kg", "片", "个", "颗", "只", "根", "枝",
            "勺", "把", "盒", "瓶", "张", "瓣", "条", "适量", "少许",
        )

        /**
         * 解析一行食材。
         *
         * 支持「厚切吐司 2片」「鲜牛奶 80ml」「肉桂粉 适量」。
         * 也容忍用空格分隔写法的「鲜牛奶 80 ml」。
         */
        fun parseIngredientLine(line: String, uid: Long): EditableIngredient {
            // 纯描述量（没有数字）
            val rawMatch = Regex("""^(\S+)\s+(适量|少许|若干)$""").find(line)
            if (rawMatch != null) {
                return EditableIngredient(
                    uid = uid,
                    name = rawMatch.groupValues[1],
                    amountText = rawMatch.groupValues[2],
                    isRawAmount = true,
                )
            }

            // 名称 + 数字 + 单位（单位可紧贴数字，也可用空格隔开）
            val m = Regex("""^(.+?)\s+([\d.]+)\s*([^\d\s]*)\s*$""").find(line)
            if (m != null) {
                val name = m.groupValues[1].trim()
                val amount = m.groupValues[2]
                val unit = m.groupValues[3].ifBlank { "克" }
                if (unit in UNIT_WORDS || unit.isBlank()) {
                    return EditableIngredient(
                        uid = uid,
                        name = name,
                        amountText = amount,
                        unit = unit,
                        isRawAmount = false,
                    )
                }
            }

            // 兜底：整行当名称，数量记为「适量」。宁可粗略也不丢弃用户粘贴的内容。
            return EditableIngredient(uid = uid, name = line, amountText = "适量", isRawAmount = true)
        }
    }
}
