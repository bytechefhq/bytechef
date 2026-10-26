dependencies {
    api(project(":server:ee:libs:platform:platform-component:platform-component-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-component:platform-component-api"))

    implementation("org.springframework:spring-context")
    implementation("tools.jackson.core:jackson-databind")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))

    testImplementation("org.springframework.ai:spring-ai-model")
    testImplementation(project(":server:libs:modules:components:ai:agent"))
    testImplementation(project(":server:libs:modules:components:ai:llm"))
    testImplementation(project(":server:libs:modules:components:data-mapper"))
    testImplementation(project(":server:libs:modules:components:http-client"))
    testImplementation(project(":server:libs:modules:components:schedule"))
    testImplementation(project(":server:libs:modules:components:webhook"))
    testImplementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))
    testImplementation(project(":server:libs:test:test-support"))
}
