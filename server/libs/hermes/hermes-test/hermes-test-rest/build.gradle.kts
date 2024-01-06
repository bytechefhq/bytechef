plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.hermes.test.web.rest")
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
    modelPackage.set("com.bytechef.hermes.test.web.rest.model")
    outputDir.set("$projectDir/generated")
    schemaMappings.set(
        mapOf(
            "ComponentDefinitionBasic" to "com.bytechef.hermes.configuration.web.rest.model.ComponentDefinitionBasic",
            "ExecutionError" to "com.bytechef.hermes.execution.web.rest.model.ExecutionErrorModel",
            "Job" to "com.bytechef.hermes.execution.web.rest.model.JobModel",
            "JobBasic" to "com.bytechef.hermes.execution.web.rest.model.JobBasicModel",
            "TaskExecution" to "com.bytechef.hermes.execution.web.rest.model.TaskExecutionModel",
            "TriggerExecution" to "com.bytechef.hermes.execution.web.rest.model.TriggerExecutionModel",
            "Webhook" to "com.bytechef.hermes.execution.web.rest.model.WebhookModel",
            "WebhookRetry" to "com.bytechef.hermes.execution.web.rest.model.WebhookRetryModel",
            "WorkflowBasic" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowBasicModel",
            "WorkflowConnection" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowConnectionModel",
            "WorkflowTask" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowTaskModel",
            "WorkflowTrigger" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowTriggerModel"
        )
    )
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    outputDir.set("$rootDir/client/src/middleware/hermes/test")
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
    annotationProcessor(libs.org.mapstruct.mapstruct.processor)
    annotationProcessor(libs.org.mapstruct.extensions.spring.mapstruct.spring.extensions)

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.apache.commons:commons-lang3")
    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.org.mapstruct)
    implementation(libs.org.mapstruct.extensions.spring.mapstruct.spring.annotations)
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-rest:hermes-execution-rest-api"))
    implementation(project(":server:libs:hermes:hermes-test:hermes-test-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}
