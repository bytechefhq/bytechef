dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.ai:spring-ai-model-chat-memory-repository-redis")
    implementation("redis.clients:jedis")
    implementation(libs.org.springaicommunity.spring.ai.session)
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:modules:components:ai:agent:chat-memory:chat-memory-session"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
    implementation(project(":spring-ai:spring-ai-session-redis"))
}
