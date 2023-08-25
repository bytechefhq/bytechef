plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.hermes.configuration.web.rest")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8-localdatetime",
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true"
        )
    )
    generatorName.set("spring")
    inputSpec.set( "$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.hermes.configuration.web.rest.model")
    outputDir.set("$projectDir/generated")
    schemaMappings.set(
        mapOf(
            "Page" to "org.springframework.data.domain.Page"
        )
    )
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    outputDir.set("$rootDir/client/src/middleware/hermes/configuration")
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
    implementation(project(":server:libs:hermes:hermes-configuration:hermes-configuration-api"))
    implementation(project(":server:libs:hermes:hermes-test-executor:hermes-test-executor-api"))
}
