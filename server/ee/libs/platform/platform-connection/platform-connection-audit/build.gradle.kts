dependencies {
    implementation("io.micrometer:micrometer-core")
    implementation("org.aspectj:aspectjweaver")
    implementation("org.springframework.boot:spring-boot-actuator")
    implementation("org.springframework:spring-aop")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(project(":server:ee:libs:platform:platform-audit:platform-audit-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))

    testImplementation("org.springframework.security:spring-security-core")
}
