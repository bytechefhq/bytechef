plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.platform.workflow.execution.web.rest")
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
    modelPackage.set("com.bytechef.platform.workflow.execution.web.rest.model")
    outputDir.set("$projectDir/../platform-workflow-execution-rest-api/generated")
    schemaMappings.set(
        mapOf(
            "ComponentDefinitionBasic" to "com.bytechef.platform.configuration.web.rest.model.ComponentDefinitionBasicModel",
            "DataStreamComponent" to "com.bytechef.platform.configuration.web.rest.model.DataStreamComponentModel",
            "Page" to "org.springframework.data.domain.Page",
            "WorkflowConnection" to "com.bytechef.platform.configuration.web.rest.model.WorkflowConnectionModel",
            "WorkflowTask" to "com.bytechef.platform.configuration.web.rest.model.WorkflowTaskModel",
            "WorkflowTrigger" to "com.bytechef.platform.configuration.web.rest.model.WorkflowTriggerModel"
        )
    )
}

val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    outputDir.set("$rootDir/client/src/shared/middleware/platform/workflow/execution")
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
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-rest:platform-workflow-execution-rest-api"))
    implementation(project(":server:libs:platform:platform-oauth2:platform-oauth2-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-registry:platform-workflow-task-dispatcher-registry-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}
