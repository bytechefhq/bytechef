dependencies {
    api(project(":server:libs:platform:platform-api"))
    api(project(":server:libs:platform:platform-user:platform-user-api"))

    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-util"))
}
