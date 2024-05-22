dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.security:spring-security-config")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:libs:embedded:embedded-user:embedded-user-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.security:spring-security-test")
}
