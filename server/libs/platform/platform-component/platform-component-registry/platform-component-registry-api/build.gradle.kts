dependencies {
    api(project(":server:libs:platform:platform-registry-api"))
    api(project(":server:sdks:java:component-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    api(project(":server:libs:platform:platform-component:platform-component-api"))
}
