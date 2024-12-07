version="1.0"

dependencies {
    implementation("org.springframework.ai:spring-ai-core:${rootProject.libs.versions.spring.ai.get()}")
    implementation(project(":server:libs:core:commons:commons-util"))
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:llm"))
    }
}
