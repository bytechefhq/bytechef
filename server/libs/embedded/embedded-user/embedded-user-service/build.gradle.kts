dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:embedded:embedded-user:embedded-user-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
