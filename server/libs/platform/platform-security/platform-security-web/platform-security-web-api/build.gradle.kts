dependencies {
    api("org.springframework.security:spring-security-config")
    api("org.springframework.security:spring-security-web")
    api(project(":server:libs:platform:platform-security:platform-security-api"))
    api(project(":server:libs:platform:platform-user:platform-user-api"))

    implementation(project(":server:libs:core:tenant:tenant-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
