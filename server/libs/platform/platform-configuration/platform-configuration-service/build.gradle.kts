dependencies {
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator"))
    implementation(project(":server:libs:platform:platform-oauth2:platform-oauth2-api"))

    testImplementation(project(":server:libs:core:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
