dependencies {
    api(project(":server:libs:core:event:event-api"))

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:message-broker:message-broker-api"))
}
