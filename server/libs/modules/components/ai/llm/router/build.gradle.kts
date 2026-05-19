version="1.0"

dependencies {
    api("org.springframework:spring-web")
    api("org.springframework:spring-webflux")
    api("org.springframework.ai:spring-ai-openai")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:llm:router"))
    }
}
