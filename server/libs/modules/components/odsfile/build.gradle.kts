version="1.0"

dependencies {
    implementation(libs.com.github.miachm.sods.sods)

    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    testImplementation(project(":server:libs:hermes:hermes-component:hermes-component-test-int-support"))
}
