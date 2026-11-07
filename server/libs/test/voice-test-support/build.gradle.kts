dependencies {
    implementation("org.assertj:assertj-core")
    implementation("org.junit.jupiter:junit-jupiter")
    implementation("org.mockito:mockito-core")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:test:test-support"))
}
