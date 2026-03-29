dependencies {
    implementation(project(":server:libs:ai:ai-agent:ai-agent-eval:ai-agent-eval-api"))
    implementation(project(":server:libs:ai:ai-agent:ai-agent-eval:ai-agent-eval-file-storage:ai-agent-eval-file-storage-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
}
