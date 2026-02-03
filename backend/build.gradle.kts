import com.github.gradle.node.NodeExtension
import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.spring") version "2.0.0"
    kotlin("plugin.jpa") version "2.0.0"
    id("com.github.node-gradle.node") version "7.0.1"
}

group = "com.investlab"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

// #region agent log
fun dbgLog(hypothesisId: String, message: String, data: Map<String, Any?> = emptyMap()) {
    try {
        val dataJson = data.entries.joinToString(prefix = "{", postfix = "}") { entry ->
            "\"${entry.key}\":\"${entry.value}\""
        }
        val json = """{"sessionId":"debug-session","runId":"pre-fix","hypothesisId":"$hypothesisId","location":"backend/build.gradle.kts","message":"$message","data":$dataJson,"timestamp":${System.currentTimeMillis()}}"""
        file("/Users/TomanJiang/SourceCode/investlab/.cursor/debug.log").appendText(json + "\n")
    } catch (_: Exception) {
        // swallow instrumentation errors
    }
}
// #endregion

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.apache.pdfbox:pdfbox:3.0.3")

    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("org.hibernate.orm:hibernate-community-dialects:6.4.4.Final")

    // Flyway
    implementation("org.flywaydb:flyway-core:10.21.0")

    // HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.13")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE") ?: "dev")
}

node {
    version = "25.3.0"
    download = true
    nodeProjectDir = file("${rootProject.projectDir}/frontend")
    // #region agent log
    dbgLog("H1", "node extension configured", mapOf("version" to version, "download" to download, "projectDir" to nodeProjectDir))
    // #endregion
}

val npmBuild by tasks.registering(NpmTask::class) {
    dependsOn("npmInstall")
    workingDir = file("${rootProject.projectDir}/frontend")
    args.set(listOf("run", "build"))
    // #region agent log
    doFirst {
        val nodeExt = project.extensions.getByType(NodeExtension::class.java)
        dbgLog(
            "H1",
            "npmBuild start",
            mapOf(
                "workingDir" to workingDir.get().asFile.absolutePath,
                "nodeVersion" to nodeExt.version,
                "download" to nodeExt.download,
                "nodeWorkDir" to nodeExt.workDir.orNull?.asFile?.absolutePath,
                "npmWorkDir" to nodeExt.npmWorkDir.orNull?.asFile?.absolutePath
            )
        )
    }
    doLast {
        dbgLog("H1", "npmBuild end", mapOf("status" to "completed"))
    }
    // #endregion
}

val cleanFrontendStatic by tasks.registering(Delete::class) {
    delete(file("${project.projectDir}/src/main/resources/static"))
}

val copyFrontend by tasks.registering(Copy::class) {
    dependsOn(cleanFrontendStatic, npmBuild)
    from("${rootProject.projectDir}/frontend/dist")
    into("${project.projectDir}/src/main/resources/static")
    // #region agent log
    doFirst { dbgLog("H2", "copyFrontend start", mapOf("from" to "${rootProject.projectDir}/frontend/dist", "into" to "${project.projectDir}/src/main/resources/static")) }
    doLast { dbgLog("H2", "copyFrontend end", mapOf("copied" to true)) }
    // #endregion
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(copyFrontend)
    // #region agent log
    doFirst { dbgLog("H3", "processResources start", mapOf("dependsOnCopyFrontend" to true)) }
    doLast { dbgLog("H3", "processResources end", mapOf("status" to "completed")) }
    // #endregion
}
