dependencies {
    implementation(project(":server:libs:hermes:hermes-scheduler:hermes-scheduler-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.quartz-scheduler:quartz")
    implementation(project(":server:libs:core:commons:commons-util"))
}
