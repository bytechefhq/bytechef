dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:platform:platform-data-table:platform-data-table-api"))

    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-util"))
}
