dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation(project(":server:libs:config:security-config"))
}
