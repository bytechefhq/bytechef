version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:automation:automation-data-table:automation-data-table-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
