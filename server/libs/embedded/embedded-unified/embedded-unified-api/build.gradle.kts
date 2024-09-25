dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":sdks:backend:java:component-api"))
    api(project(":server:libs:platform:platform-connection:platform-connection-api"))

    implementation("org.springframework:spring-core")
}
