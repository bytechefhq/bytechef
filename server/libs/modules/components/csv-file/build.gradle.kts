version="1.0"

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-test-int-support"))
    testImplementation(project(":server:libs:platform:platform-file-storage:platform-file-storage-api"))
    testImplementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
}
