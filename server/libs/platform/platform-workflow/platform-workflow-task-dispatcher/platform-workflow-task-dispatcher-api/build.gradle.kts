dependencies {
    api(project(":sdks:backend:java:definition-api"))
    api(project(":server:libs:platform:platform-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))
}
