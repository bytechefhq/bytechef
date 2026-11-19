version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    testImplementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl"))
    testImplementation(project(":server:libs:core:commons:commons-util"))
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:test:test-support"))
}
