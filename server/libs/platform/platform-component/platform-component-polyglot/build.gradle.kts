dependencies {
    api(rootProject.libs.org.graalvm.polyglot.polyglot)

    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(rootProject.libs.org.graalvm.polyglot.js)
    testImplementation(rootProject.libs.org.graalvm.polyglot.ruby)
}
