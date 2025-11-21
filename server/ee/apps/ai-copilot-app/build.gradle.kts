group = "com.bytechef.ai.copilot"
description = ""

springBoot {
    mainClass.set("com.bytechef.ai.copilot.AiCopilotApplication")
}

dependencies {
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:jdbc-config"))

    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-rest"))
    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-service"))
    implementation(project(":server:ee:libs:config:observability-config"))

    runtimeOnly("com.zaxxer:HikariCP")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":server:libs:test:test-int-support"))
}
