plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.platform.user.web.rest")
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
    modelPackage.set("com.bytechef.platform.user.web.rest.model")
    outputDir.set("$projectDir/../platform-user-rest-api/generated")
    schemaMappings.set(
        mapOf(
            "Page" to "org.springframework.data.domain.Page"
        )
    )
}

tasks.register("generateOpenAPI") {
    dependsOn(generateOpenAPISpring)
}


dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-rest:platform-rest-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-rest:platform-user-rest-api"))

    testImplementation("com.zaxxer:HikariCP")
    testImplementation("org.springframework:spring-tx")
    testImplementation("org.springframework.boot:spring-boot-starter-mail")
    testImplementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(project(":server:libs:config:cache-config"))
    testImplementation(project(":server:libs:config:jdbc-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:config:messages-config"))
    testImplementation(project(":server:libs:config:security-config"))
    testImplementation(project(":server:libs:core:tenant:tenant-single-service"))
    testImplementation(project(":server:libs:platform:platform-rest:platform-rest-impl"))
    testImplementation(project(":server:libs:platform:platform-user:platform-user-service"))
    testImplementation(project(":server:libs:test:test-int-support"))

    testImplementation(project(":server:ee:libs:core:tenant:tenant-multi-service"))
    testImplementation(project(":server:ee:libs:config:tenant-multi-data-config"))
    testImplementation(project(":server:ee:libs:config:tenant-multi-security-config"))
}
