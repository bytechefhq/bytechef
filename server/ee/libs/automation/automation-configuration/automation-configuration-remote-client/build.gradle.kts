dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))

    implementation(project(":server:ee:libs:core:commons:commons-rest-client"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
