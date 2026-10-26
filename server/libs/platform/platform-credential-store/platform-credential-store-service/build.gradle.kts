dependencies {
    api(project(":server:libs:platform:platform-credential-store:platform-credential-store-api"))

    implementation("org.springframework:spring-context")

    testImplementation(project(":server:libs:test:test-support"))
}
