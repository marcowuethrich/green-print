plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.quarkus)
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.quarkusBom))
    implementation(libs.quarkus.restJackson)
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.kotlin)
    testImplementation(libs.quarkus.junit)
}

group = "ch.wuethrich"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    jvmToolchain(25)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
