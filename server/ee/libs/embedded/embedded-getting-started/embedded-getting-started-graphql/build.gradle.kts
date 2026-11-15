dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.graphql:spring-graphql")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:platform:platform-api"))

    implementation(project(":server:ee:libs:embedded:embedded-getting-started:embedded-getting-started-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
}
