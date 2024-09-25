dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:platform:platform-security:platform-security-web:platform-security-web-api"))
}
