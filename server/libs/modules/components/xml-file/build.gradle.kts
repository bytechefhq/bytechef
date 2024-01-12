version="1.0"

dependencies {
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    testImplementation(project(":server:libs:core:commons:commons-util"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-test-int-support"))
    testImplementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
}
