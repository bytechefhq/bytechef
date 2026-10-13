dependencies {
    api(project(":server:libs:platform:platform-component:platform-component-api"))

    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:test:test-support"))
}
