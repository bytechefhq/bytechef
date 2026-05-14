dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.ai:spring-ai-model-chat-memory-repository-jdbc")
    implementation("redis.clients:jedis")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:modules:components:ai:agent:chat-memory:chat-memory-jdbc"))
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
