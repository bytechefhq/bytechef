dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-workspace"))
    api(project(":server:libs:platform:platform-ai:platform-ai-api"))
    api("org.springframework.ai:spring-ai-client-chat")
    api("org.springframework.ai:spring-ai-model")

    api(project(":server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-api"))

    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
