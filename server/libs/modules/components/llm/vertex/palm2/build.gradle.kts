version="1.0"

dependencies {
    implementation("org.springframework.ai:spring-ai-vertex-ai-palm2-spring-boot-starter:1.0.0-M2")
    implementation(project(":server:libs:modules:components:llm"))
    testImplementation(project(":server:libs:modules:components:llm", "test"))
}
