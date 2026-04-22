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
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway-otlp:platform-ai-gateway-otlp-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-observability:automation-ai-observability-api"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-cache")
    testImplementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:core:encryption:encryption-api"))
    testImplementation(project(":server:libs:core:encryption:encryption-impl"))
    testImplementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    testImplementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))
    testImplementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    testImplementation(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-service"))
    testImplementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service"))
    testImplementation(project(":server:ee:libs:automation:automation-ai:automation-ai-observability:automation-ai-observability-api"))
    testImplementation(project(":server:ee:libs:automation:automation-ai:automation-ai-observability:automation-ai-observability-service"))
    testImplementation(project(":server:ee:libs:automation:automation-ai:automation-ai-prompt:automation-ai-prompt-service"))
    testImplementation(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-service"))
    testImplementation(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-api"))
    testImplementation(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service"))
    testImplementation(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway-otlp:platform-ai-gateway-otlp-service"))
    testImplementation(project(":server:ee:libs:platform:platform-ai:platform-ai-llm-usage:platform-ai-llm-usage-service"))
    testImplementation(project(":server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-api"))
    testImplementation(project(":server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-service"))
    testImplementation(project(":server:ee:libs:platform:platform-ai:platform-ai-prompt:platform-ai-prompt-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
    testImplementation("org.springframework.data:spring-data-jdbc")
}
