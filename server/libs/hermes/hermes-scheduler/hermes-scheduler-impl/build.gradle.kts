dependencies {
    api(project(":server:libs:hermes:hermes-scheduler:hermes-scheduler-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.quartz-scheduler:quartz")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-definition-registry:hermes-definition-registry-api"))
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))
}
