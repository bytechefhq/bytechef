plugins {
    id("com.bytechef.java-library-conventions")
    alias(libs.plugins.org.openapi.generator)
}

val generateClient by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("java")
    library.set("native")
    inputSpec.set("$projectDir/openapi.yaml")
    outputDir.set("$projectDir/generated")
    apiPackage.set("com.bytechef.cli.client.automation.api")
    modelPackage.set("com.bytechef.cli.client.automation.model")
    invokerPackage.set("com.bytechef.cli.client.automation")
    modelNameSuffix.set("Model")
    configOptions.set(
        mapOf(
            "useJakartaEe" to "true",
            "useTags" to "true",
            "hideGenerationTimestamp" to "true",
            "openApiNullable" to "false"
        )
    )
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

// The generated client sources are committed; regenerate manually with the `generateClient` task
// when openapi.yaml changes (mirrors the server public-rest modules — not wired to compileJava).

// Generated client sources are not subject to ByteChef style/quality rules.
listOf("checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest").forEach { taskName ->
    tasks.matching { it.name == taskName }
        .configureEach { enabled = false }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("jakarta.annotation:jakarta.annotation-api")

    // The openapi-generator "native" library uses Apache HttpMime to encode the multipart
    // project-file upload (deployProject); it is not otherwise on the classpath.
    implementation("org.apache.httpcomponents:httpmime:4.5.14")
}
