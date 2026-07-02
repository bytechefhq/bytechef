plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.ee.embedded.configuration.public_.web.rest")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useEnumCaseInsensitive" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true"
        )
    )
    generatorName.set("spring")
    inputSpec.set( "$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.ee.embedded.configuration.public_.web.rest.model")
    outputDir.set("$projectDir/generated")
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

tasks.register("generateOpenAPI") {
    dependsOn(generateOpenAPISpring)
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
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":spring-ai:spring-ag-ui:packages:core"))
    implementation(project(":spring-ai:spring-ag-ui:packages:server"))
    implementation(project(":spring-ai:spring-ag-ui:servers:spring"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:rest:rest-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-tool"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-api"))

    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-mcp:platform-mcp-api"))

    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-api"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-api"))
    implementation(project(":server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
    testImplementation(project(":spring-ai:spring-ag-ui:utils:json"))
}
