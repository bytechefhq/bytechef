version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:platform:platform-data-table:platform-data-table-api"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
