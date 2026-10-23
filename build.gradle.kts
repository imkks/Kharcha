// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    detekt {
        config.setFrom(files("$rootDir/detekt-config.yml"))
    }
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            exclude("**/test/**")
            exclude("**/androidTest/**")
        }
    }
    tasks.matching {
        it.name.contains("Test", ignoreCase = true) && it.name.contains("ktlint", ignoreCase = true)
    }.configureEach {
        enabled = false
    }
}
