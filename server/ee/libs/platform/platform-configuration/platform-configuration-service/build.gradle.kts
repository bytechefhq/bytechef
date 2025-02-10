dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-api"))
}
