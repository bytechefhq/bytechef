dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.data:spring-data-commons")
    implementation(project(":server:libs:platform:platform-rest:platform-rest-api"))
    implementation(project(":server:libs:core:encryption:encryption-api"))

    testImplementation("jakarta.validation:jakarta.validation-api")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.security:spring-security-test")
}
