dependencies {
    api("org.springframework.data:spring-data-commons")

    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
