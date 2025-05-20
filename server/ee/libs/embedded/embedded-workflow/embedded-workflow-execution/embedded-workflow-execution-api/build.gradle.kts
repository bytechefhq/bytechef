dependencies {
    api(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    api(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-api"))

    implementation("org.springframework.data:spring-data-relational")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
