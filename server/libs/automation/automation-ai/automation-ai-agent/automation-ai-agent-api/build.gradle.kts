dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    api(project(":server:libs:core:exception:exception-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:libs:platform:platform-tag:platform-tag-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
}
