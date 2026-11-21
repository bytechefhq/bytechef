dependencies {
    api(project(":sdks:backend:java:embedded:integration-api"))
    api(project(":server:ee:libs:platform:platform-code-workflow:platform-code-workflow-loader"))

    testImplementation(project(":server:libs:platform:platform-component:platform-component-polyglot"))
}

tasks.withType<Test>().configureEach {
    systemProperty("bytechef.sdks.dir", "$rootDir/sdks")
}
