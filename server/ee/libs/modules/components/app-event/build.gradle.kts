dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-api"))
}
