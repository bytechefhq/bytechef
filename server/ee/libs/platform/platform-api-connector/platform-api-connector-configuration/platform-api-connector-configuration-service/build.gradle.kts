dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation(libs.io.swagger.parser.v3.swagger.parser)
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(project(":cli:commands:component:init:openapi"))
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))

    implementation(project(":server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-api"))
    implementation(project(":server:ee:libs:platform:platform-api-connector:platform-api-connector-file-storage:platform-api-connector-file-storage-api"))


    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
