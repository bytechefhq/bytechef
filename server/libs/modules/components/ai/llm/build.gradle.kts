version="1.0"

dependencies {
    api("org.springframework.ai:spring-ai-client-chat")
    api("org.springframework.boot:spring-boot")

    implementation("org.springframework.ai:spring-ai-retry")
    implementation("org.springframework.boot:spring-boot-http-client")
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:llm"))
    }
}
