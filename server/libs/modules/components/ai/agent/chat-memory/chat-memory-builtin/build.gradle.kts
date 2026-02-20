dependencies {
    implementation("org.springframework.ai:spring-ai-model-chat-memory-repository-jdbc")
    implementation("org.springframework.ai:spring-ai-model-chat-memory-repository-redis:2.0.0-M1")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("redis.clients:jedis")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
