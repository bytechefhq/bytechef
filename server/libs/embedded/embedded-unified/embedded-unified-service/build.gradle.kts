dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:libs:embedded:embedded-connected-user:embedded-connected-user-api"))
    implementation(project(":server:libs:embedded:embedded-unified:embedded-unified-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
