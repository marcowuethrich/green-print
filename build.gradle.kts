plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader. Quarkus is deliberately not listed: on this shared classpath
    // its dependency constraints upgrade AGP's jaxb-runtime 2.3 to 4.x, which drops the javax.xml.bind
    // API AGP's SDK loader needs on JDK 11+. `:server` loads it on its own classpath instead.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt) apply false
}

// Captured here: the type-safe `libs` accessor is not resolvable inside `subprojects {}`.
val composeRulesKtlint = libs.compose.rules.ktlint
val ktlintEngineVersion = libs.versions.ktlint.engine.get()
val detektKotlinVersion = libs.versions.detekt.kotlin.get()

// Zero-findings quality gate (ADR-016): ktlint with the Compose rules and detekt on every module,
// configured in .editorconfig and config/detekt/detekt.yml. No baselines: any finding fails the build.
ktlint {
    version.set(ktlintEngineVersion)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    dependencies {
        "ktlintRuleset"(composeRulesKtlint)
    }

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(ktlintEngineVersion)
        // Compose resources and SQLDelight register build/generated/** as source directories; that
        // code is not ours to format.
        filter {
            exclude { it.file.path.contains("${File.separator}generated${File.separator}") }
        }
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        source.setFrom(files("src"))
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
    }
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        reports.html.required.set(false)
        reports.xml.required.set(false)
        reports.sarif.required.set(false)
        reports.md.required.set(false)
        reports.txt.required.set(true)
        // Detekt 1.23 embeds a Kotlin 2.0 compiler that cannot read a JDK 25 home or target it. Without
        // a JDK home detekt parses sources only, which is all the rules enabled here need.
        jdkHome.unsetConvention()
        jdkHome.unset()
        jvmTarget = "21"
    }
    // The per-source-set tasks (detektMain, detektJvmMain, detektMetadataCommonMain, ...) add type
    // resolution, which needs the JDK home unset above. `detekt` covers the whole src/ tree and is the
    // gate; re-enable these once detekt 2.x (which reads JDK 25) is stable.
    tasks
        .matching {
            (it is io.gitlab.arturbosch.detekt.Detekt || it is io.gitlab.arturbosch.detekt.DetektCreateBaselineTask) &&
                it.name != "detekt"
        }.configureEach { enabled = false }
    // Detekt 1.23 refuses to run on any Kotlin compiler but the one it was built with; the Kotlin
    // plugin would otherwise align its classpath to the project's Kotlin version.
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion(detektKotlinVersion)
        }
    }
}

tasks.register("prePRcheck") {
    group = "verification"
    description =
        "The local PR gate: ktlint and detekt on every module, the shared, Compose UI and server " +
        "tests, and the Android debug and wasm builds."
    dependsOn(":ktlintCheck")
    dependsOn(subprojects.map { "${it.path}:ktlintCheck" })
    dependsOn(subprojects.map { "${it.path}:detekt" })
    dependsOn(
        ":shared:jvmTest",
        ":composeApp:jvmTest",
        ":server:test",
        ":androidApp:assembleDebug",
        ":composeApp:wasmJsBrowserDistribution",
    )
}
