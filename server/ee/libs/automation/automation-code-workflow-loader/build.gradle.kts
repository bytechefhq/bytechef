dependencies {
    api(project(":sdks:backend:java:workflow-api"))

    api(project(":server:ee:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-api"))

    implementation(rootProject.libs.org.graalvm.polyglot.polyglot)
    implementation(rootProject.libs.org.graalvm.polyglot.java)
    implementation(rootProject.libs.org.graalvm.polyglot.js)
    implementation(rootProject.libs.org.graalvm.polyglot.python)
    implementation(rootProject.libs.org.graalvm.polyglot.ruby)
    implementation(project(":server:libs:core:class-loader:class-loader-api"))
}
