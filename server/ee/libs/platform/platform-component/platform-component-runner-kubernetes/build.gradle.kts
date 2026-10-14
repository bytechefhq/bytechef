dependencies {
    implementation("io.fabric8:kubernetes-client")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api"))

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-k3s")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation(project(":server:ee:libs:modules:components:kubernetes"))
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-api"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl"))
    testImplementation(project(":server:libs:test:test-support"))
}
