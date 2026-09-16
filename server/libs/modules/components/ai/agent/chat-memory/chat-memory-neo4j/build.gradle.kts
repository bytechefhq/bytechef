dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework.ai:spring-ai-model-chat-memory-repository-neo4j")
    implementation(project(":server:libs:core:commons:commons-util"))
}
