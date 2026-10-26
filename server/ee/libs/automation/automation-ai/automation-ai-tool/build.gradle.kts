dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-core")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:ee:libs:automation:automation-context-store:automation-context-store-api"))
    implementation(project(":server:ee:libs:platform:platform-context-store:platform-context-store-api"))
    implementation(project(":server:libs:ai:ai-api"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-tool"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation(project(":server:libs:test:test-support"))
}
