dependencies {
    api("io.projectreactor:reactor-core")

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-core")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
}
