dependencies {
    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
