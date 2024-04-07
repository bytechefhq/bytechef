dependencies {
    implementation(project(":server:libs:core:tag:tag-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
