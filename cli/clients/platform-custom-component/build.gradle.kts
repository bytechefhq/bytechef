plugins {
    id("com.bytechef.java-library-conventions")
    alias(libs.plugins.org.openapi.generator)
}

val generateClient by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("java")
    library.set("native")
    inputSpec.set(
        "${rootDir}/server/ee/libs/platform/platform-custom-component/" +
            "platform-custom-component-configuration/platform-custom-component-configuration-rest/openapi.yaml"
    )
    outputDir.set("$projectDir/generated")
    apiPackage.set("com.bytechef.cli.client.platformcustomcomponent.api")
    modelPackage.set("com.bytechef.cli.client.platformcustomcomponent.model")
    invokerPackage.set("com.bytechef.cli.client.platformcustomcomponent")
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

listOf("checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest").forEach { taskName ->
    tasks.matching { it.name == taskName }
        .configureEach { enabled = false }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("jakarta.annotation:jakarta.annotation-api")

    implementation("org.apache.httpcomponents:httpmime:4.5.14")
}
