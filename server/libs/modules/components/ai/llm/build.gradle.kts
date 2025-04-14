version="1.0"

dependencies {
    api("org.springframework.ai:spring-ai-client-chat:${rootProject.libs.versions.spring.ai.get()}")
    api("org.springframework.boot:spring-boot")

    implementation("org.springframework.ai:spring-ai-retry:${rootProject.libs.versions.spring.ai.get()}")
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:llm"))
    }
}
