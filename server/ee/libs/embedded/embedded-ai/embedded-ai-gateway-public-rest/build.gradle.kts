plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring =
    tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateOpenAPISpring") {
        configOptions.set(
            mapOf(
                "useEnumCaseInsensitive" to "true",
                "useSpringBoot3" to "true"
            )
        )
        generatorName.set("spring")
        globalProperties.set(
            mapOf(
                "modelDocs" to "false",
                "modelTests" to "false",
                "models" to ""
            )
        )
        inputSpec.set("$projectDir/openapi.yaml")
        modelNameSuffix.set("Model")
        modelPackage.set("com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model")
        outputDir.set("$projectDir/generated")
    }

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

tasks.register("generateOpenAPI") {
    dependsOn(generateOpenAPISpring)
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-core")
    testImplementation(project(":server:libs:test:test-support"))
}
