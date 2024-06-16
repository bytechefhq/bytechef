dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
    implementation(project(":server:ee:libs:core:discovery:discovery-util"))
}
