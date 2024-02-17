dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.batch:spring-batch-core")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    testImplementation("org.springframework.batch:spring-batch-test")
}
