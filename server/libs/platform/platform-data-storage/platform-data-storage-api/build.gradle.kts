dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:platform:platform-api"))
    api(project(":sdks:backend:java:component-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-relational")
}
