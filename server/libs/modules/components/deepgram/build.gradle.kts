version="1.0"

dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    testImplementation(project(":server:libs:test:voice-test-support"))
}
