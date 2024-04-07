dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:platform:platform-rest:platform-rest-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation("jakarta.validation:jakarta.validation-api")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.security:spring-security-test")
}
