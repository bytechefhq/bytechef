dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":sdks:backend:java:component-api"))
    api(project(":server:libs:platform:platform-api"))
    api(project(":server:libs:platform:platform-tag:platform-tag-api"))
    api(project(":server:libs:platform:platform-credential-store:platform-credential-store-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-actuator")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:test:test-support"))
}
