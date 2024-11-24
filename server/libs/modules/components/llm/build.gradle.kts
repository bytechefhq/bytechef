version="1.0"

dependencies {
    implementation("org.springframework.ai:spring-ai-openai:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.junit.jupiter:junit-jupiter-api")
    implementation("org.mockito:mockito-core")
    implementation(project(":server:libs:core:commons:commons-util"))
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:llm"))

        testImplementation(project(":server:libs:modules:components:llm"))
    }
}
