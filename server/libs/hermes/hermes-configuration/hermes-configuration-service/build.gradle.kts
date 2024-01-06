dependencies {
    implementation(project(":server:libs:hermes:hermes-configuration:hermes-configuration-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-oauth2:hermes-oauth2-api"))

    testImplementation(project(":server:libs:core:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
