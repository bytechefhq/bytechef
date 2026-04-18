dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:core:evaluator:evaluator-api"))
    api(project(":server:libs:core:exception:exception-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-relational")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:test:test-support"))
}
