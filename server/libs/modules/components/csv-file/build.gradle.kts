version="1.0"

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")

    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    testImplementation(project(":server:libs:hermes:hermes-component:hermes-component-test-int-support"))
    testImplementation(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))
}
