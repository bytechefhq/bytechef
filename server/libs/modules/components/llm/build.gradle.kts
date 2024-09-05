version="1.0"

dependencies {
    implementation("org.springframework.ai:spring-ai-openai:1.0.0-M2")
    implementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    implementation("org.mockito:mockito-core:5.11.0")
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:llm"))

        testImplementation(project(":server:libs:modules:components:llm"))
    }
}
