dependencies {
    api(project(":sdks:backend:java:automation:project-api"))
    api(project(":server:ee:libs:platform:platform-code-workflow:platform-code-workflow-loader:platform-code-workflow-loader-api"))
    api(project(":server:libs:core:class-loader:class-loader-api"))
    api(project(":server:libs:platform:platform-component:platform-component-polyglot"))
}

tasks.withType<Test>().configureEach {
    systemProperty("bytechef.sdks.dir", "$rootDir/sdks")
}
