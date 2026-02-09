dependencies {
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation("org.springframework.data:spring-data-jdbc")

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
