dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:libs:platform:platform-connection:platform-connection-api"))

    testImplementation(project(":server:libs:core:commons:commons-data"))
}
