version="1.0"

dependencies {
    implementation("org.springframework.ai:spring-ai-core:${rootProject.libs.versions.spring.ai.get()}")
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:llm"))
    }
}
