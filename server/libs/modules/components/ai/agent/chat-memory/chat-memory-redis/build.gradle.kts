dependencies {
    implementation("redis.clients:jedis")
    implementation("org.springframework.ai:spring-ai-model-chat-memory-repository-redis:2.0.0-M1")
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
