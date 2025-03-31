version="1.0.0"

dependencies {
    api("org.springframework.ai:spring-ai-core:${rootProject.libs.versions.spring.ai.get()}")

    implementation(project(":sdks:backend:java:ai-toolkit"))
}
