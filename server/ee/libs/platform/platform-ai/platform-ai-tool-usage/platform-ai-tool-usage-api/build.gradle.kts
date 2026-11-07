dependencies {
    api("org.springframework.ai:spring-ai-model")
    api("org.springframework.data:spring-data-jdbc")
    api("io.micrometer:micrometer-core")
    api("tools.jackson.core:jackson-databind")

    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
