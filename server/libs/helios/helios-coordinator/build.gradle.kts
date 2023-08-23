dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:helios:helios-configuration:helios-configuration-api"))
    implementation(project(":server:libs:helios:helios-execution:helios-execution-api"))
    implementation(project(":server:libs:hermes:hermes-coordinator:hermes-coordinator-api"))
}
