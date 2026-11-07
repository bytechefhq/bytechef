dependencies {
    api(project(":server:ee:libs:platform:platform-knowledge-base:platform-knowledge-base-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-knowledge-base:platform-knowledge-base-api"))
    api("org.springframework.ai:spring-ai-vector-store")

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
}
