dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-api"))
}
