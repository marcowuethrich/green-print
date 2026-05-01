plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinAllOpen)
    alias(libs.plugins.quarkus)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.enterprise.context.RequestScoped")
    annotation("io.quarkus.security.Authenticated")
    annotation("io.quarkus.hibernate.reactive.panache.common.WithSession")
    annotation("io.quarkus.hibernate.reactive.panache.common.WithTransaction")
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.quarkusBom))
    implementation(libs.jackson.module.kotlin)
    implementation(libs.quarkus.restClient)
    implementation(libs.quarkus.restJackson)
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.oidc)
    implementation(libs.quarkus.hibernate.reactive.panache.kotlin)
    implementation(libs.quarkus.reactive.pg.client)
    implementation(projects.shared)

    testImplementation(libs.quarkus.junit5)
    testImplementation(libs.rest.assured)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.quarkus.test.security.oidc)
    testImplementation(libs.quarkus.test.vertx)
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        javaParameters.set(true)
    }
}
