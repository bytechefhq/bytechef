dependencies {
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.data:spring-data-commons")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))
    implementation(project(":server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
