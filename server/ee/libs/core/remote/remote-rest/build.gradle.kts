dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:platform:platform-tenant:platform-tenant-api"))
}
