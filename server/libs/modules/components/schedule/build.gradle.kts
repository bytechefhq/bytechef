version="1.0"

dependencies {
    implementation(project(":server:libs:hermes:hermes-scheduler:hermes-scheduler-api"))

    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
}
