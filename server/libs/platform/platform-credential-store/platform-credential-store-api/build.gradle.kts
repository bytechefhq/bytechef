dependencies {
    api(project(":server:libs:core:exception:exception-api"))

    implementation("org.apache.commons:commons-lang3")

    testImplementation(project(":server:libs:test:test-support"))
}
