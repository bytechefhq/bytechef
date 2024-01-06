plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.helios.configuration.web.rest")
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
    modelPackage.set("com.bytechef.helios.configuration.web.rest.model")
    outputDir.set("$projectDir/../helios-configuration-rest-api/generated")
    schemaMappings.set(
        mapOf(
            "Workflow" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowModel",
            "WorkflowBasic" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowBasicModel",
            "WorkflowConnection" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowConnectionModel",
            "WorkflowFormat" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowFormatModel",
            "WorkflowInput" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowInputModel",
            "WorkflowOutput" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowOutputModel",
            "WorkflowTask" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowTaskModel",
            "WorkflowTrigger" to "com.bytechef.hermes.configuration.web.rest.model.WorkflowTriggerModel"
        )
    )
}

val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    outputDir.set("$rootDir/client/src/middleware/helios/configuration")
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
    implementation(project(":server:libs:helios:helios-configuration:helios-configuration-rest:helios-configuration-rest-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}
