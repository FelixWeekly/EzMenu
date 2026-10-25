# EzMenu 混淆规则（release 构建用）

# Compose 自带 consumer rules，通常无需额外配置。
# Room 生成的实现类按需保留（Room 的 consumer rules 已覆盖，这里作为兜底）。
-keep class com.ezmenu.app.data.local.** { *; }

# 保留行号，便于崩溃栈定位
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
