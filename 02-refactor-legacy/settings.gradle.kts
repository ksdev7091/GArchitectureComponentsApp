pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LegacyTodoRefactor"

// 各ステップは独立したアプリモジュール。隣り合うステップを Android Studio の
// 「Compare Files」で並べて見比べながら学習できる。
include(":step0-legacy")
include(":step1-repository")
include(":step2-viewmodel")
include(":step3-room-flow")
include(":step4-hilt-test")
include(":step5-compose")
