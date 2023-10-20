dependencies {
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-scheduler:hermes-scheduler-api"))
}
