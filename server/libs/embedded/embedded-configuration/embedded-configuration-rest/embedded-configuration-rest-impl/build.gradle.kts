plugins {
    alias(libs.plugins.org.openapi.generator)
}


val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.embedded.configuration.web.rest")
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
    modelPackage.set("com.bytechef.embedded.configuration.web.rest.model")
    outputDir.set("$projectDir/../embedded-configuration-rest-api/generated")
    schemaMappings.set(
        mapOf(
            "CredentialStatus" to "com.bytechef.platform.connection.web.rest.model.CredentialStatusModel",
            "DataStreamComponent" to "com.bytechef.platform.configuration.web.rest.model.DataStreamComponentModel",
            "Page" to "org.springframework.data.domain.Page",
        )
    )
}

val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    outputDir.set("$rootDir/client/src/shared/middleware/embedded/configuration")
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
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-rest:platform-connection-rest-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}
