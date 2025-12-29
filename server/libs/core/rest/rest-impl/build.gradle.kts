dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation("org.springframework.data:spring-data-commons")
    implementation(project(":server:libs:core:encryption:encryption-api"))
    implementation(project(":server:libs:core:rest:rest-api"))

    testImplementation("jakarta.validation:jakarta.validation-api")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(project(":server:libs:test:test-int-support"))
}
