dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:core:autoconfigure-annotations"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))
    implementation(project(":server:libs:hermes:hermes-file-storage:hermes-file-storage-api"))
    implementation(project(":server:libs:hermes:hermes-webhook:hermes-webhook-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-web")
}
