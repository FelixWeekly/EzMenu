// EzMenu (旬食) — Gradle 设置
//
// 仓库顺序：国内镜像优先（dl.google.com 可能被墙），google()/mavenCentral() 兜底。
// 全部构件均已实测在国内镜像上可达，见计划 §一。

pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/google") { name = "aliyunGoogle" }
        maven("https://maven.aliyun.com/repository/public") { name = "aliyunPublic" }
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google") { name = "aliyunGoogle" }
        maven("https://maven.aliyun.com/repository/public") { name = "aliyunPublic" }
        mavenCentral()
        google()
    }
}

rootProject.name = "EzMenu"
include(":app")
