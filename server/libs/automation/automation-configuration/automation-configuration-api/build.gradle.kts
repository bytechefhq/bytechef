dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:platform:platform-category:platform-category-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:libs:platform:platform-connection:platform-connection-api"))
    api(project(":server:libs:platform:platform-security:platform-security-api"))
    api(project(":server:libs:platform:platform-tag:platform-tag-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
}
