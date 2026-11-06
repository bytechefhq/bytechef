dependencies {
    api("org.springframework.data:spring-data-jdbc")
    api(project(":server:libs:platform:platform-connection:platform-connection-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-api"))

    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
