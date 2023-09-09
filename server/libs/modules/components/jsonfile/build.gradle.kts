version="1.0"

dependencies {
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    testImplementation(project(":server:libs:core:commons:commons-util"))
    testImplementation(project(":server:libs:hermes:hermes-component:hermes-component-test-int-support"))
    testImplementation(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))
}
