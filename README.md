# 旬食 · EzMenu 2.0

离线食谱、烹饪计时与料理手记。

Kotlin、Jetpack Compose、Room 与 DataStore 构建的中文 Android 离线应用。没有账号和网络依赖；食谱、照片与手记都存在本机。

## 功能

- **食谱**：编写、搜索、分类、收藏、草稿和排序。编辑自动暂存，正式内容在保存后更新。
- **灵感**：16 道明确标注的内置食谱；收录为个人副本后再修改。按真实余料匹配，支持自由输入与持久化。
- **手记**：完成一餐保存当时的完整配方与实际份量；补充最多三张照片、心得和下次调整。之后修改或删除原菜谱，不会覆盖历史。
- **我的**：真实统计、厨房昵称、默认份量、屏幕常亮、浅深主题、回收站与完整备份恢复。

烹饪会话、步骤与各步计时持久化；切走后可继续。计时按截止时刻计算，到时提醒按需申请通知与准确闹钟权限。设备重启后明确提示会话中断；系统强制停止应用期间不能保证提醒。

导出提供预览卡片、完整分页 PDF，以及 Markdown 与图片 ZIP。分享使用系统面板；备份经过版本、清单与校验检查，恢复前确认替换并保留恢复前副本。

## 构建与验证

最低 Android 8.0 / API 26；compileSdk / targetSdk 36。现有依赖主版本保持不变。

本机验证配置：JDK 21（E:/jdk21）、Android SDK（E:/sdk_AS）。JDK 路径不再写入公共 Gradle 配置；SDK 路径使用本地、不纳入分发的 local.properties。

```powershell
$env:JAVA_HOME = 'E:/jdk21'
.\gradlew.bat :app:assembleDebug
# 或运行完整构建、单测与 Lint；加 -Device 执行已连接设备测试
.\tools/verify.ps1 -JavaHome 'E:/jdk21' -Device
# 已安装测试 APK 后，验证 360 / 412 / 840dp、浅深色和 200% 字体
python tools/visual_matrix.py --adb E:/sdk_AS/platform-tools/adb.exe --serial emulator-5554 --output shots/identity/matrix
```

Debug APK 在 app/build/outputs/apk/debug/，Release 产物在 app/build/outputs/apk/release/。公开发布前应使用自己的发布密钥签名；项目不保存发布密钥。

## 数据与工程

数据库 v2 提供显式 1→2 迁移，不使用破坏性回退。旧版食谱、收藏、草稿和记录保留；没有历史配方的旧记录不会被伪造补全。新安装个人记录与库存从零开始。

RecipeSnapshot 保存原始食材数值、步骤与媒体引用；CookingSessionRepository 完成事务以 sessionId 去重；ArchiveRepository 提供校验、恢复前副本和中断回滚。

列表查询仅加载卡片信息与标签，按页扩大查询窗口；完整配方只在详情、编辑与会话中读取。图片采样、EXIF 方向校正和导出都在后台处理。

首页保留「旬食」品牌标识。标题与正文字体都包含完整常用中文，字重为真实的 400/500/600，避免同一菜名中混入系统黑体。封面共享过渡、按压、收藏、导航和步骤切换跟随系统动画倍率；关闭动画后操作仍然可用。字体重建：python tools/build_reading_fonts.py（源字体缓存见脚本）。

- 新界面：app/src/main/java/com/ezmenu/app/ui/studio/
- 编辑器：ui/screens/editor/，独立编辑草稿不覆盖正式菜谱
- 数据：data/local/、data/repository/；历史快照：domain/model/
- 内置封面：res/drawable-nodpi/editorial_*.webp；导出：util/RecipeExport.kt

[设计规范](DESIGN.md) · [验证结果](VERIFICATION.md) · [作品案例](PORTFOLIO.md) · [第三方来源](THIRD_PARTY_NOTICES.md)

旧 reference/ 原型作为设计演变记录保留，新实现以 DESIGN.md 为基准。
