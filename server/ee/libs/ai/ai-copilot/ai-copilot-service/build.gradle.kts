dependencies {
    implementation(libs.com.github.mizosoft.methanol)
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.ai:spring-ai-commons")
    implementation("org.springframework.ai:spring-ai-pgvector-store")
    implementation("org.springframework.ai:spring-ai-autoconfigure-vector-store-pgvector")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation("org.springframework.ai:spring-ai-advisors-vector-store")
    implementation("org.springframework.ai:spring-ai-anthropic")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(files("../libs/ag-ui"))  // Using extracted classes from core, json, server, spring-ai JARs
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))
    implementation(project(":server:libs:ai:mcp:mcp-tool:mcp-tool-automation"))
    implementation(project(":server:libs:ai:mcp:mcp-tool:mcp-tool-platform"))

    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-api"))
}
