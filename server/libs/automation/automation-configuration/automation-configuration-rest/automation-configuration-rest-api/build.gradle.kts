sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

dependencies {
    annotationProcessor(libs.org.mapstruct.mapstruct.processor)
    annotationProcessor(libs.org.mapstruct.extensions.spring.mapstruct.spring.extensions)

    api(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.org.mapstruct)
    implementation(libs.org.mapstruct.extensions.spring.mapstruct.spring.annotations)
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:rest:rest-api"))
}
