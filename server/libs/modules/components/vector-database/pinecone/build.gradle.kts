version="1.0"

dependencies {
    implementation("io.grpc:grpc-netty:1.69.0")
    implementation("org.springframework.ai:spring-ai-openai:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.springframework.ai:spring-ai-pinecone-store:${rootProject.libs.versions.spring.ai.get()}")
}
