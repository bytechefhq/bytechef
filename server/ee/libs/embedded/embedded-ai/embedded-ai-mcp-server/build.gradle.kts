dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.ai:spring-ai-mcp")
    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))

    implementation(project(":server:ee:libs:embedded:embedded-execution:embedded-execution-api"))
}
