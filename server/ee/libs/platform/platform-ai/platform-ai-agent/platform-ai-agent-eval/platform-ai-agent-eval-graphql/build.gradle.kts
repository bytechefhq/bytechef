dependencies {
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-eval:platform-ai-agent-eval-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-eval:platform-ai-agent-eval-file-storage:platform-ai-agent-eval-file-storage-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
}
