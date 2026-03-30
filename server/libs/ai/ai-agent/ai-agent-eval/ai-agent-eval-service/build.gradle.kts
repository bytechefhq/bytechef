dependencies {
    implementation(project(":server:libs:ai:ai-agent:ai-agent-eval:ai-agent-eval-api"))
    implementation(project(":server:libs:ai:ai-agent:ai-agent-eval:ai-agent-eval-file-storage:ai-agent-eval-file-storage-api"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:config:liquibase-config"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-api"))

    implementation(libs.org.springaicommunity.agent.judge.core)
    implementation(libs.org.springaicommunity.agent.judge.llm)
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
}
