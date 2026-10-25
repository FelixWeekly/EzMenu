# 素材与组件来源

EzMenu 2.0 的 14 张食物示意图由本项目于 2026-10-23 使用 OpenAI imagegen 生成，压缩为 1200×800 WebP 后打包于 res/drawable-nodpi/editorial_*.webp。图像包含 13 种成品菜与一张三文鱼腌制步骤，不代表用户真实下厨记录。原型中的远程照片不随应用分发；用户导入的照片保留在用户本地档案。

| 资源 | 项目 | 许可 / 来源 |
|---|---|---|
| 中文衬线字形 | Noto Serif SC | SIL Open Font License 1.1；https://github.com/notofonts/noto-cjk |
| 中文无衬线字形 | Noto Sans SC | SIL Open Font License 1.1；https://github.com/google/fonts/tree/main/ofl/notosanssc |
| 拉丁衬线字形 | Newsreader | SIL Open Font License 1.1；https://github.com/productiontype/Newsreader |
| 拉丁无衬线字形 | Manrope | SIL Open Font License 1.1；https://github.com/sharanda/manrope |
| 图标字形 | Material Symbols | Apache License 2.0；https://github.com/google/material-design-icons |
| Android 界面与数据 | AndroidX / Jetpack | Apache License 2.0；https://android.googlesource.com/platform/frameworks/support/ |
| 本地图片加载 | Coil | Apache License 2.0；https://github.com/coil-kt/coil |
| 协程 | kotlinx.coroutines | Apache License 2.0；https://github.com/Kotlin/kotlinx.coroutines |

中英标题使用 Noto Serif SC / Newsreader，正文使用 Noto Sans SC / Manrope。400、500、600 三个字重分别静态实例化，不使用合成粗体。字库覆盖完整 GB2312 字表及项目出现的其他汉字，构建与字形校验保留在 tools/build_reading_fonts.py，原始字体由 Google Fonts 官方仓库获取。品牌标记由原生 Canvas 绘制。应用不使用远程图片或在线字体。
