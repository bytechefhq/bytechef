dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-api"))
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))
    implementation(project(":server:libs:hermes:hermes-webhook:hermes-webhook-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-web")
}
