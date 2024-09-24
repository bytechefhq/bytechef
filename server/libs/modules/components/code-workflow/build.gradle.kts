version="1.0"

dependencies {
    implementation(rootProject.libs.org.graalvm.polyglot.polyglot)
    implementation(rootProject.libs.org.graalvm.polyglot.java)
    implementation(rootProject.libs.org.graalvm.polyglot.js)
    implementation(rootProject.libs.org.graalvm.polyglot.python)
    implementation(rootProject.libs.org.graalvm.polyglot.ruby)
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-api"))
    implementation(project(":server:libs:platform:platform-code-workflow:platform-code-workflow-file-storage:platform-code-workflow-file-storage-api"))
    implementation(project(":server:libs:platform:platform-code-workflow:platform-code-workflow-loader:platform-code-workflow-loader-automation"))

    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    testImplementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-test-int-support"))
}
