plugins {
    alias(libs.plugins.org.openapi.generator)
}


val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.ee.embedded.configuration.web.rest")
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
    modelPackage.set("com.bytechef.ee.embedded.configuration.web.rest.model")
    outputDir.set("$projectDir/../embedded-configuration-rest-api/generated")
    schemaMappings.set(
        mapOf(
            "Page" to "org.springframework.data.domain.Page",
            "WorkflowConnection" to "com.bytechef.platform.configuration.web.rest.model.WorkflowConnectionModel",
            "WorkflowFormat" to "com.bytechef.platform.configuration.web.rest.model.WorkflowFormatModel",
            "WorkflowInput" to "WorkflowInputModel",
            "WorkflowOutput" to "WorkflowOutputModel",
            "WorkflowTask" to "WorkflowTaskModel",
            "WorkflowTrigger" to "WorkflowTriggerModel",
        )
    )
    importMappings.set(
        mapOf(
            "WorkflowInputModel" to "com.bytechef.platform.configuration.web.rest.model.WorkflowInputModel",
            "WorkflowOutputModel" to "com.bytechef.platform.configuration.web.rest.model.WorkflowOutputModel",
            "WorkflowTaskModel" to "com.bytechef.platform.configuration.web.rest.model.WorkflowTaskModel",
            "WorkflowTriggerModel" to "com.bytechef.platform.configuration.web.rest.model.WorkflowTriggerModel"
        )
    )
}

val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    outputDir.set("$rootDir/client/src/ee/shared/middleware/embedded/configuration")
}

tasks.register("generateOpenAPI") {
    dependsOn(generateOpenAPISpring, generateOpenAPITypeScriptFetch)
}

dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(project(":server:libs:test:test-int-support"))
}
