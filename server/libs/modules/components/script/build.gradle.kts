version="1.0"

dependencies {
    implementation(rootProject.libs.org.graalvm.polyglot.polyglot)
    implementation(rootProject.libs.org.graalvm.polyglot.java)
    implementation(rootProject.libs.org.graalvm.polyglot.js)
    implementation(rootProject.libs.org.graalvm.polyglot.python)
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-polyglot"))
    implementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-context:platform-component-context-api"))

    // The definition snapshot is assembled from the enabled task runners, so the test builds an
    // ApplicationProperties of its own rather than inheriting whatever the classpath happens to supply.
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    testImplementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
}
