version="1.0"

dependencies {
    testImplementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-impl"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-memory"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-service"))
    testImplementation(project(":server:libs:core:tenant:tenant-api"))
    testImplementation(project(":server:libs:modules:task-dispatchers:condition"))
}
