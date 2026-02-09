dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.yaml:snakeyaml")
    implementation(libs.io.swagger.parser.v3.swagger.parser)
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(project(":cli:commands:component:init:openapi"))
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))

    implementation(project(":server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-api"))
    implementation(project(":server:ee:libs:platform:platform-api-connector:platform-api-connector-file-storage:platform-api-connector-file-storage-api"))


    implementation("org.springframework.data:spring-data-jdbc")

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
