version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api"))

    // ProcessTaskRunner, GraalVmTaskRunner and TaskRunnerRegistryImpl are needed to build a real, deterministic
    // registry for the definition snapshot test, mirroring script's ScriptComponentHandlerTest arrangement.
    testImplementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl"))
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:test:test-support"))
}
