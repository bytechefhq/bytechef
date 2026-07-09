dependencies {
    api(libs.org.springaicommunity.mcp.server.security)
    api("org.springframework.security:spring-security-config")
    api("org.springframework.security:spring-security-web")
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework:spring-test")
}
