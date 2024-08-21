dependencies {
    api("org.springframework.security:spring-security-config")
    api("org.springframework.security:spring-security-web")
    api(project(":server:libs:platform:platform-security:platform-security-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")

}
