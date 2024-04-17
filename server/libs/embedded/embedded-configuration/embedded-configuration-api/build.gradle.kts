dependencies {
    api(project(":server:libs:platform:platform-category:platform-category-api"))
    api(project(":server:libs:platform:platform-tag:platform-tag-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
}
