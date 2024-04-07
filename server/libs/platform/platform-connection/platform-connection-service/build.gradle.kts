dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    api(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:sdks:java:component-api"))
    implementation(project(":server:libs:platform:platform-oauth2:platform-oauth2-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-instance-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))

    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:core:encryption:encryption-impl"))
    testImplementation(project(":server:libs:core:tag:tag-service"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
