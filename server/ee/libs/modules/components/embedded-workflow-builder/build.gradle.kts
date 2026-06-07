version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:ee:libs:embedded:embedded-execution:embedded-execution-api"))
}
