plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.ee.embedded.workflow.execution.web.rest")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true"
        )
    )
    generatorName.set("spring")
    inputSpec.set( "$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.ee.embedded.workflow.execution.web.rest.model")
    outputDir.set("$projectDir/generated")
    schemaMappings.set(
        mapOf(
            "Page" to "org.springframework.data.domain.Page",
            "Category" to "com.bytechef.ee.embedded.configuration.web.rest.model.CategoryModel",
            "ComponentConnection" to "com.bytechef.platform.configuration.web.rest.model.ComponentConnection",
            "ComponentDefinitionBasic" to "com.bytechef.platform.configuration.web.rest.model.ComponentDefinitionBasic",
            "Environment" to "com.bytechef.ee.embedded.configuration.web.rest.model.EnvironmentModel",
            "ExecutionError" to "com.bytechef.platform.workflow.execution.web.rest.model.ExecutionErrorModel",
            "IntegrationBasic" to "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationBasicModel",
            "IntegrationInstanceBasic" to  "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceBasicModel",
            "IntegrationInstanceConfiguration" to  "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel",
            "IntegrationInstanceConfigurationWorkflow" to "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationWorkflowModel",
            "IntegrationInstanceConfigurationWorkflowConnection" to "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationWorkflowConnectionModel",
            "IntegrationInstanceConfigurationBasic" to  "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationBasicModel",
            "IntegrationInstance" to  "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceModel",
            "IntegrationInstance_integration" to "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceIntegrationModel",
            "IntegrationInstanceWorkflowConnection" to "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceWorkflowConnectionModel",
            "IntegrationInstanceWorkflow" to "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceWorkflowModel",
            "Integration" to "com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationModel",
            "Job" to "com.bytechef.platform.workflow.execution.web.rest.model.JobModel",
            "JobBasic" to "com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel",
            "Page" to "org.springframework.data.domain.Page",
            "Tag" to "com.bytechef.ee.embedded.configuration.web.rest.model.TagModel",
            "TaskExecution" to "com.bytechef.platform.workflow.execution.web.rest.model.TaskExecutionModel",
            "TriggerExecution" to "com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel",
            "Webhook" to "com.bytechef.platform.workflow.execution.web.rest.model.WebhookModel",
            "WebhookRetry" to "com.bytechef.platform.workflow.execution.web.rest.model.WebhookRetryModel",
            "WorkflowBasic" to "com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel",
            "WorkflowConnection" to "com.bytechef.platform.configuration.web.rest.model.WorkflowConnectionModel",
            "WorkflowTask" to "com.bytechef.platform.configuration.web.rest.model.WorkflowTaskModel",
            "WorkflowTrigger" to "com.bytechef.platform.configuration.web.rest.model.WorkflowTriggerModel"
        )
    )
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    outputDir.set("$rootDir/client/src/ee/shared/middleware/embedded/workflow/execution")
}

tasks.register("generateOpenAPI") {
    dependsOn(generateOpenAPISpring, generateOpenAPITypeScriptFetch)
}

dependencies {
    annotationProcessor(libs.org.mapstruct.mapstruct.processor)
    annotationProcessor(libs.org.mapstruct.extensions.spring.mapstruct.spring.extensions)

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.org.mapstruct)
    implementation(libs.org.mapstruct.extensions.spring.mapstruct.spring.annotations)
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:rest:rest-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-rest:platform-workflow-execution-rest-api"))

    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-api"))
    implementation(project(":server:ee:libs:embedded:embedded-workflow:embedded-workflow-execution:embedded-workflow-execution-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(project(":server:libs:test:test-int-support"))
}
