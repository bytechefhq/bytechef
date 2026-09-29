dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.ai:spring-ai-model-chat-memory-repository-redis")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation("org.springframework.ai:spring-ai-autoconfigure-model-chat-memory-repository-redis")
}
