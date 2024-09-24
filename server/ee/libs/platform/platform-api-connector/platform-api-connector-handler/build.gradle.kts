dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    implementation(project(":server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-api"))
    implementation(project(":server:ee:libs:platform:platform-api-connector:platform-api-connector-file-storage:platform-api-connector-file-storage-api"))
}
