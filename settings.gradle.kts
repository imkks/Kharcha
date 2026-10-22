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

rootProject.name = "Kharcha"

include(":app")
include(":core:model")
include(":core:database")
include(":core:common")
include(":core:designsystem")
include(":feature:onboarding")
include(":feature:home")
include(":feature:transactions")
include(":feature:review")
include(":feature:insights")
include(":feature:accounts")
include(":feature:settings")
include(":engine:sms")
include(":engine:parser")
include(":engine:classifier")
include(":engine:categorizer")
include(":engine:recurring")
