version="1.0"

dependencies {
    api("org.springframework.ai:spring-ai-client-chat")
    api("org.springframework.boot:spring-boot")

    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-api"))
    implementation("org.springframework.ai:spring-ai-retry")
    implementation("org.springframework.boot:spring-boot-http-client")
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:llm"))
    }
}
