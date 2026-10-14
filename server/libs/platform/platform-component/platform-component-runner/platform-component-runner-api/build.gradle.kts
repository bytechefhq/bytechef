dependencies {
    api(project(":server:libs:platform:platform-component:platform-component-api"))
    api(project(":sdks:backend:java:component-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
}
