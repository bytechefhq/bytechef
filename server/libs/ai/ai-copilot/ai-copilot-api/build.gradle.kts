dependencies {
    api("io.projectreactor:reactor-core")

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-core")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("tools.jackson.core:jackson-databind")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":spring-ai:spring-ag-ui:packages:core"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
}
