dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))

    implementation(project(":server:ee:libs:core:commons:commons-rest-client"))
}
