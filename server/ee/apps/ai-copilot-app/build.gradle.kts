group = "com.bytechef.ai.copilot"
description = ""

springBoot {
    mainClass.set("com.bytechef.ai.copilot.CopilotApplication")
}

dependencies {
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-agent-api"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-tool"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-tool"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-tool"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-tool"))
    implementation(project(":server:libs:automation:automation-connection:automation-connection-tool"))
    implementation(project(":server:libs:automation:automation-data-table:automation-data-table-tool"))
    implementation(project(":server:libs:automation:automation-knowledge-base:automation-knowledge-base-tool"))
    implementation(project(":server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-tool"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:jdbc-config"))

    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-rest"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-service"))
    implementation(project(":server:ee:libs:config:observability-config"))

    runtimeOnly("com.zaxxer:HikariCP")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":server:libs:test:test-int-support"))
}
