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
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-service"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:cache-config"))
    implementation(project(":server:libs:config:jdbc-config"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql"))

    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-rest"))
    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-service"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-api"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-graphql"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-rest"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-service"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-copilot"))
    // Registers the optional OpenNLP-backed SensitiveDataDetector when an operator enables and configures it.
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp"))
    implementation(project(":server:ee:libs:config:observability-config"))

    runtimeOnly("com.zaxxer:HikariCP")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":server:libs:test:test-int-support"))
}
