dependencies {
    implementation(project(":server:libs:ai:ai-agent:ai-agent-skill:ai-agent-skill-api"))
    implementation(project(":server:libs:ai:ai-agent:ai-agent-skill:ai-agent-skill-file-storage:ai-agent-skill-file-storage-api"))
    implementation(project(":server:libs:config:liquibase-config"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
}
