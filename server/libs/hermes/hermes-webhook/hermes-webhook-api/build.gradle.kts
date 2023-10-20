dependencies {
    api(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))

    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-component:hermes-component-api"))
}
