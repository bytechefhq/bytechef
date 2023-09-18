plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.helios.execution.web.rest")
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
    modelPackage.set("com.bytechef.helios.execution.web.rest.model")
    outputDir.set("$projectDir/generated")
    schemaMappings.set(
        mapOf(
            "Page" to "org.springframework.data.domain.Page",
            "ProjectBasic" to "com.bytechef.helios.configuration.web.rest.model.ProjectBasicModel",
            "ProjectInstanceBasic" to  "com.bytechef.helios.configuration.web.rest.model.ProjectInstanceBasicModel",
            "ProjectInstance" to  "com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel",
            "ProjectInstance_project" to "com.bytechef.helios.configuration.web.rest.model.ProjectInstanceProjectModel",
            "ProjectInstanceWorkflowConnection" to "com.bytechef.helios.configuration.web.rest.model.ProjectInstanceWorkflowConnectionModel",
            "ProjectInstanceWorkflow" to "com.bytechef.helios.configuration.web.rest.model.ProjectInstanceWorkflowModel",
            "Project" to "com.bytechef.helios.configuration.web.rest.model.ProjectModel",
            "WorkflowTask" to "com.bytechef.helios.configuration.web.rest.model.WorkflowTaskModel"
        )
    )
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    outputDir.set("$rootDir/client/src/middleware/helios/execution")
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

    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.org.mapstruct)
    implementation(libs.org.mapstruct.extensions.spring.mapstruct.spring.annotations)
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:core:autoconfigure-annotations"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:helios:helios-configuration:helios-configuration-rest:helios-configuration-rest-api"))
    implementation(project(":server:libs:helios:helios-execution:helios-execution-api"))
    implementation(project(":server:libs:hermes:hermes-test-executor:hermes-test-executor-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}
