dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.ai:spring-ai-model-chat-memory-repository-redis")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:tenant:tenant-api"))

    testImplementation("org.springframework.ai:spring-ai-autoconfigure-model-chat-memory-repository-redis")
}
