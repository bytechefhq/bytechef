dependencies {
    api(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    api(project(":server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-api"))

    implementation("org.springframework.data:spring-data-relational")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator"))
}
