dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("tools.jackson.core:jackson-databind")
    testImplementation(project(":server:libs:test:test-support"))
}
