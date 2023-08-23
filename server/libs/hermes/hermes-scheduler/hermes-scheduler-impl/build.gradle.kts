dependencies {
    api(project(":server:libs:hermes:hermes-scheduler:hermes-scheduler-api"))

    implementation(libs.com.github.kagkarlsson.db.scheduler)
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-definition-registry:hermes-definition-registry-api"))
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))
}
