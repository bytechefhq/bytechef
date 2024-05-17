dependencies {
    api(project(":server:libs:platform:platform-api"))
    api(project(":sdks:backend:java:component-api"))
    api(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    api(project(":server:libs:platform:platform-component:platform-component-api"))
}
