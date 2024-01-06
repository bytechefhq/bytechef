dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-connection:hermes-connection-api"))
}
