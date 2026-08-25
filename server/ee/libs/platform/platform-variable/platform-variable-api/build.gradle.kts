dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
