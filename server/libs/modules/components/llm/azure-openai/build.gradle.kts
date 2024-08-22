version="1.0"

dependencies {
    implementation("org.springframework.ai:spring-ai-azure-openai-spring-boot-starter:1.0.0-SNAPSHOT")
    implementation(project(":server:libs:modules:components:llm"))
    testImplementation(project(":server:libs:modules:components:llm", "test"))
}
