dependencies {
    implementation("redis.clients:jedis")
    implementation("org.springframework.ai:spring-ai-model-chat-memory-repository-redis")
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
