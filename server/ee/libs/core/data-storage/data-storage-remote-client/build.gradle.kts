dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:data-storage:data-storage-api"))

    implementation(project(":server:ee:libs:core:commons:commons-webclient"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
