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
}
