plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpringManagement by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.ee.automation.app.public_.web.rest.management")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true"
        )
    )
    generatorName.set("spring")
    inputSpec.set("$projectDir/openapi-management.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.ee.automation.app.public_.web.rest.management.model")
    outputDir.set("$projectDir/generated")
    apiFilesConstrainedTo.set(listOf(""))
    modelFilesConstrainedTo.set(listOf(""))
    supportingFilesConstrainedTo.set(listOf("ApiUtil.java"))
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.ee.automation.app.public_.web.rest")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true"
        )
    )
    generatorName.set("spring")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.ee.automation.app.public_.web.rest.model")
    outputDir.set("$projectDir/generated")
    apiFilesConstrainedTo.set(listOf(""))
    modelFilesConstrainedTo.set(listOf(""))
    supportingFilesConstrainedTo.set(listOf("ApiUtil.java"))
    mustRunAfter(generateOpenAPISpringManagement)
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

tasks.register("generateOpenAPI") {
    dependsOn(generateOpenAPISpringManagement, generateOpenAPISpring)
}

dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:core:rest:rest-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-file-storage:platform-file-storage-api"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-api"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-api"))
    implementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-api"))
    implementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-handler:automation-api-platform-handler-rest"))
    implementation(project(":server:ee:libs:automation:automation-app:automation-app-api"))

    testImplementation("org.hibernate.validator:hibernate-validator")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(project(":server:libs:core:rest:rest-impl"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-api"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
}
