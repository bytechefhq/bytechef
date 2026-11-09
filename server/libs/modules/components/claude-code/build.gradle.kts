version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(libs.org.springaicommunity.agents.agent.claude)
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-workspace"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation("org.springframework.security:spring-security-core")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:core:commons:commons-util"))
    testImplementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    testImplementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-file-storage:platform-ai-skill-file-storage-api"))
    testImplementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-file-storage:platform-ai-skill-file-storage-impl"))
    testImplementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-service"))
    testImplementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
