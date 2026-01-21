version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:automation:automation-data-table:automation-data-table-configuration:automation-data-table-configuration-api"))
    implementation(project(":server:libs:automation:automation-data-table:automation-data-table-execution:automation-data-table-execution-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
