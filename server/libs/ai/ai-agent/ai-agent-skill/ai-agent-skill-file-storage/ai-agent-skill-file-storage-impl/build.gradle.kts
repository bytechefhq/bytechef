dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:ai:ai-agent:ai-agent-skill:ai-agent-skill-file-storage:ai-agent-skill-file-storage-api"))
}
