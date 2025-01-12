version="1.0"

dependencies {
    implementation("org.springframework.ai:spring-ai-openai:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.springframework.ai:spring-ai-markdown-document-reader:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.springframework.ai:spring-ai-pdf-document-reader:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.springframework.ai:spring-ai-tika-document-reader:${rootProject.libs.versions.spring.ai.get()}")
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:vector-store"))
    }
}
