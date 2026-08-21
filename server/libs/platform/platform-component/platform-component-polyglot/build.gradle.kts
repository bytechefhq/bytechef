dependencies {
    api(rootProject.libs.org.graalvm.polyglot.polyglot)

    implementation("io.micrometer:context-propagation")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(rootProject.libs.org.graalvm.polyglot.js)
    testImplementation(rootProject.libs.org.graalvm.polyglot.python)
    // RUBY-DISABLED: polyglot ruby is stuck at 25.0.0 and crashes on the pinned Truffle 25.2.4. Re-enable
    // together with the org-graalvm-polyglot-ruby entry in gradle/libs.versions.toml once a ruby jar built on
    // Truffle 25.2+ is published (or GraalVM is downgraded). Grep RUBY-DISABLED for every site.
//    testImplementation(rootProject.libs.org.graalvm.polyglot.ruby)
}
