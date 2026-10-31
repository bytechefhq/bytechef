dependencies {
    api("org.springframework.data:spring-data-jdbc")
    api(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
