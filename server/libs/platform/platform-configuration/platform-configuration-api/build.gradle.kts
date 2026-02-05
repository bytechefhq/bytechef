dependencies {
    api(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    api(project(":server:libs:core:error:error-api"))
    api(project(":server:libs:platform:platform-component:platform-component-api"))
    api(project(":server:libs:platform:platform-component:platform-component-log:platform-component-log-api"))
    api(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
}
