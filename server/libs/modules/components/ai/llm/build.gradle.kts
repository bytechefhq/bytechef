version="1.0"

dependencies {
    api("org.springframework.ai:spring-ai-client-chat")
    api("org.springframework.boot:spring-boot")
    api(project(":server:libs:platform:platform-ai:platform-ai-api"))

    implementation("org.springframework.ai:spring-ai-retry")
    implementation("org.springframework.boot:spring-boot-http-client")
    implementation(project(":server:libs:core:commons:commons-util"))
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:llm"))
    }
}
