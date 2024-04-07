dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:platform:platform-api"))
    api(project(":server:libs:platform:platform-tag:platform-tag-api"))

    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
