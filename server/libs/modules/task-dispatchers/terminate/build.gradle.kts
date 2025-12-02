version="1.0"

dependencies {
    testImplementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-impl"))
    testImplementation(project(":server:libs:modules:task-dispatchers:loop"))
    testImplementation(project(":server:libs:modules:task-dispatchers:on-error"))
}
