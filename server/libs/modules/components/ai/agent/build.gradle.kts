dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:libs:modules:components:ai:llm"))
}
