dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:helios:helios-configuration:helios-configuration-api"))

    implementation(project(":ee:server:libs:core:commons:commons-webclient"))
}
