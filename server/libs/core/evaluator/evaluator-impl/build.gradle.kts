dependencies {
    api(project(":server:libs:core:evaluator:evaluator-api"))

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-expression")

    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:core:commons:commons-util"))
    testImplementation(project(":server:libs:test:test-support"))
}
