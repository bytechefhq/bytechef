plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.ee.automation.ai.gateway.public_.web.rest")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useEnumCaseInsensitive" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true"
        )
    )
    generatorName.set("spring")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.ee.automation.ai.gateway.public_.web.rest.model")
    outputDir.set("$projectDir/generated")
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

tasks.register("generateOpenAPI") {
    dependsOn(generateOpenAPISpring)
}

dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
}
