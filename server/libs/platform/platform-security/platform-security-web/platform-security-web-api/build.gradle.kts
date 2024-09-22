dependencies {
    api("org.springframework.security:spring-security-config")
    api("org.springframework.security:spring-security-web")
    api(project(":server:libs:platform:platform-security:platform-security-api"))
    api(project(":server:libs:platform:platform-user:platform-user-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:platform:platform-tenant:platform-tenant-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
