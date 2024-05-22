dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-instance-api"))
}
