dependencies {
    implementation(project(":server:libs:licence:licence-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation("org.springframework:spring-context")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
