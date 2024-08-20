dependencies {
    api(project(":server:libs:platform:platform-api"))
    api(project(":sdks:backend:java:component-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))
}
