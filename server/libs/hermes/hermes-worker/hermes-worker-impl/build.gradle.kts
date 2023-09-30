dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-api"))
    implementation(project(":server:libs:hermes:hermes-worker:hermes-worker-api"))
    implementation(project(":server:libs:hermes:hermes-coordinator:hermes-coordinator-api"))
}
