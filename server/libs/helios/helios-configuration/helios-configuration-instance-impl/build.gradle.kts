dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:helios:helios-configuration:helios-configuration-api"))
    implementation(project(":server:libs:hermes:hermes-configuration:hermes-configuration-instance-api"))
}
