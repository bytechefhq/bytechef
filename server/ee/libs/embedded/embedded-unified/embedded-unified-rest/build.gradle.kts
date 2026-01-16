plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateAccountingOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.ee.embedded.unified.web.rest.accounting")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true"
        )
    )
    generatorName.set("spring")
    inputSpec.set( "$projectDir/openapi/v1/accounting/openapi.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.ee.embedded.unified.web.rest.accounting.model")
    outputDir.set("$projectDir/generated")
    schemaMappings.set(
        mapOf(
            "page" to "com.bytechef.ee.embedded.unified.pagination.CursorPageSlice"
        )
    )
}

val generateCRMOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.ee.embedded.unified.web.rest.crm")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true"
        )
    )
    generatorName.set("spring")
    inputSpec.set( "$projectDir/openapi/v1/crm/openapi.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.ee.embedded.unified.web.rest.crm.model")
    outputDir.set("$projectDir/generated")
    schemaMappings.set(
        mapOf(
            "page" to "com.bytechef.ee.embedded.unified.pagination.CursorPageSlice"
        )
    )
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

//val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
//    generatorName.set("typescript-fetch")
//    inputSpec.set("$projectDir/openapi.yaml")
//    modelNameSuffix.set("Model")
//    outputDir.set("$rootDir/client/src/shared/middleware/embedded/unified")
//}

tasks.register("generateOpenAPI") {
    dependsOn(generateAccountingOpenAPISpring, generateCRMOpenAPISpring/*, generateOpenAPITypeScriptFetch*/)
}

dependencies {
    annotationProcessor(libs.org.mapstruct.mapstruct.processor)
    annotationProcessor(libs.org.mapstruct.extensions.spring.mapstruct.spring.extensions)

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation("org.apache.commons:commons-lang3")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.org.mapstruct)
    implementation(libs.org.mapstruct.extensions.spring.mapstruct.spring.annotations)
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:rest:rest-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))

    implementation(project(":server:ee:libs:embedded:embedded-unified:embedded-unified-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(project(":server:libs:test:test-int-support"))
}
