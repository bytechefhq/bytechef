version="1.0"

dependencies {
    implementation("org.springframework.ai:spring-ai-openai:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.springframework.ai:spring-ai-weaviate-store:${rootProject.libs.versions.spring.ai.get()}")
}
