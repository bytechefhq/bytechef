dependencies {
    api(project(":server:libs:platform:platform-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.jspecify:jspecify")
    implementation(project(":server:libs:core:exception:exception-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
