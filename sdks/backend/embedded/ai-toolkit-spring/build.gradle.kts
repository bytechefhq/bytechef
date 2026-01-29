plugins {
    `java-library`
}

version="1.0.0"

dependencies {
    api("org.springframework.ai:spring-ai-client-chat:${rootProject.libs.versions.spring.ai.get()}")

    implementation(project(":sdks:backend:embedded:ai-toolkit"))
}
