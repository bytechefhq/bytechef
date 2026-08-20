val guestBridge: Configuration by configurations.creating

dependencies {
    api(project(":sdks:backend:java:component-api"))

    api(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-api"))

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation(rootProject.libs.org.graalvm.polyglot.polyglot)
    implementation(rootProject.libs.org.graalvm.polyglot.java)
    implementation(rootProject.libs.org.graalvm.polyglot.js)
    implementation(rootProject.libs.org.graalvm.polyglot.python)
    // RUBY-DISABLED: polyglot ruby is stuck at 25.0.0 and crashes on the pinned Truffle 25.2.4. Re-enable
    // together with the org-graalvm-polyglot-ruby entry in gradle/libs.versions.toml once a ruby jar built on
    // Truffle 25.2+ is published (or GraalVM is downgraded). Grep RUBY-DISABLED for every site.
//    implementation(rootProject.libs.org.graalvm.polyglot.ruby)
    implementation(project(":server:libs:core:class-loader:class-loader-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-polyglot"))

    guestBridge(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-guest-bridge"))

    testImplementation("org.mockito:mockito-core")
}

tasks.test {
    // GraalVM Espresso's internal `assert` statements fire spuriously while the guest JVM boots when host
    // assertions are enabled, and Gradle test tasks enable them by default.
    enableAssertions = false
}

tasks.processResources {
    from(guestBridge) {
        into("META-INF/guest-sdk/custom-component")
    }

    doLast {
        val guestBridgeDir = File(destinationDir, "META-INF/guest-sdk/custom-component")

        val jarNames = guestBridgeDir.listFiles { file: File -> file.name.endsWith(".jar") }
            .orEmpty()
            .map { it.name }
            .sorted()

        File(guestBridgeDir, "index.txt").writeText(jarNames.joinToString("\n"))
    }
}
