version="1.0"

dependencies {
    implementation("com.openai:openai-java-client-okhttp")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    testImplementation(project(":server:libs:test:voice-test-support"))
}
