dependencies {
    api(project(":server:libs:platform:platform-security:platform-security-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.springframework.security:spring-security-web")
}
