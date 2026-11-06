dependencies {
    api(project(":server:libs:platform:platform-component:platform-component-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:ee:libs:platform:platform-external-invocation-log:platform-external-invocation-log-api"))

    implementation("org.springframework:spring-core")
}
