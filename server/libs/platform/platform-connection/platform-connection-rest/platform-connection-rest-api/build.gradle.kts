plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.platform.connection.web.rest")
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
    modelPackage.set("com.bytechef.platform.connection.web.rest.model")
    outputDir.set("$projectDir/generated")
    schemaMappings.set(
        mapOf(
            "Page" to "org.springframework.data.domain.Page",
            "Tag" to "TagModel",
            "UpdateTagsRequest" to "com.bytechef.platform.tag.web.rest.model.UpdateTagsRequestModel",
        )
    )
    importMappings.set(
        mapOf(
            "TagModel" to "com.bytechef.platform.tag.web.rest.model.TagModel"
        )
    )
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    outputDir.set("$rootDir/client/src/shared/middleware/platform/connection")
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

    api(project(":server:libs:platform:platform-connection:platform-connection-api"))
    api(project(":server:libs:platform:platform-tag:platform-tag-rest:platform-tag-rest-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.apache.commons:commons-lang3")
    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation(libs.org.mapstruct)
    implementation(libs.org.mapstruct.extensions.spring.mapstruct.spring.annotations)
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:core:commons:commons-util"))
}
