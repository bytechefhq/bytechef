dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:automation:automation-data-table:automation-data-table-api"))
    api(project(":server:libs:automation:automation-data-table:automation-data-table-configuration:automation-data-table-configuration-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-util"))
}
