//plugins {
//    alias(libs.plugins.org.openapi.generator)
//}

//val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
//    apiPackage.set("com.bytechef.ee.platform.user.web.rest")
//    configOptions.set(
//        mapOf(
//            "interfaceOnly" to "true",
//            "useSpringBoot3" to "true",
//            "useTags" to "true"
//        )
//    )
//    generatorName.set("spring")
//    inputSpec.set( "$projectDir/openapi.yaml")
//    modelNameSuffix.set("Model")
//    modelPackage.set("com.bytechef.ee.platform.user.web.rest.model")
//    outputDir.set("$projectDir/generated")
//    schemaMappings.set(
//        mapOf(
//            "Page" to "org.springframework.data.domain.Page"
//        )
//    )
//}

//val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
//    generatorName.set("typescript-fetch")
//    inputSpec.set("$projectDir/openapi.yaml")
//    outputDir.set("$rootDir/client/src/ee/shared/middleware/platform/user")
//}

//sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

//tasks.register("generateOpenAPI") {
//    dependsOn(generateOpenAPISpring, generateOpenAPITypeScriptFetch)
//}

dependencies {
//    annotationProcessor(libs.org.mapstruct.mapstruct.processor)
//    annotationProcessor(libs.org.mapstruct.extensions.spring.mapstruct.spring.extensions)

//    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-mail"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    testImplementation("com.zaxxer:HikariCP")
}
