dependencies {
    api(project(":server:libs:automation:automation-search:automation-search-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
