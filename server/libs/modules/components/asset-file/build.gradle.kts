version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:automation:automation-asset-file:automation-asset-file-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
