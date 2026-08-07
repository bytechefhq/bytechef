dependencies {
    implementation(libs.io.github.a2asdk.a2a.java.sdk.client)
    implementation(libs.org.springaicommunity.spring.ai.agent.utils)
    implementation(libs.org.springaicommunity.spring.ai.agent.utils.common)
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":spring-ai:spring-ai-agent-utils:auto-memory"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:modules:components:script"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))

    testImplementation("org.mockito:mockito-core")
}
