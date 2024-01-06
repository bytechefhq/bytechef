plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.hermes.execution.web.rest")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8-localdatetime",
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true"
        )
    )
    generatorName.set("spring")
    inputSpec.set( "$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.hermes.execution.web.rest.model")
    outputDir.set("$projectDir/../hermes-execution-rest-api/generated")
    schemaMappings.set(
        mapOf(
            "ComponentDefinitionBasic" to "com.bytechef.hermes.configuration.web.rest.model.ComponentDefinitionBasicModel",
            "Page" to "org.springframework.data.domain.Page",
            "WorkflowConnection" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowConnectionModel",
            "WorkflowTask" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowTaskModel",
            "WorkflowTrigger" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowTriggerModel"
        )
    )
}

val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    outputDir.set("$rootDir/client/src/middleware/hermes/execution")
    typeMappings.set(
        mapOf(
            "DateTime" to "Date"
        )
    )
}

tasks.register("generateOpenAPI") {
    dependsOn(generateOpenAPISpring, generateOpenAPITypeScriptFetch)
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-rest:hermes-execution-rest-api"))
    implementation(project(":server:libs:hermes:hermes-oauth2:hermes-oauth2-api"))
    implementation(project(":server:libs:hermes:hermes-task-dispatcher:hermes-task-dispatcher-registry:hermes-task-dispatcher-registry-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}
