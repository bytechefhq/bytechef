dependencies {
    implementation(libs.org.springaicommunity.spring.ai.agent.utils)
    implementation(project(":server:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-skill:platform-ai-agent-skill-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
