dependencies {
    api("org.springframework.data:spring-data-jdbc")
    api(project(":server:libs:platform:platform-connection:platform-connection-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation(project(":server:libs:core:commons:commons-util"))
}
