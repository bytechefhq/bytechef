dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.apache.commons:commons-lang3")
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:embedded:embedded-connected-user:embedded-connected-user-api"))
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-web:platform-security-web-api"))
    implementation(project(":server:libs:platform:platform-tenant:platform-tenant-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
}
