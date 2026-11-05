dependencies {
    api(project(":sdks:backend:java:embedded:integration-api"))
    api(project(":sdks:backend:java:workflow-api"))

    api(project(":server:ee:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-api"))

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation(rootProject.libs.org.graalvm.polyglot.polyglot)
    implementation(rootProject.libs.org.graalvm.polyglot.java)
    implementation(rootProject.libs.org.graalvm.polyglot.js)
    implementation(rootProject.libs.org.graalvm.polyglot.python)
    implementation(project(":server:libs:core:class-loader:class-loader-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-polyglot"))
}

tasks.withType<Test>().configureEach {
    systemProperty("bytechef.sdks.dir", "$rootDir/sdks")
}
