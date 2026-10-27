dependencies {
    implementation("org.springframework:spring-aop")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
}
