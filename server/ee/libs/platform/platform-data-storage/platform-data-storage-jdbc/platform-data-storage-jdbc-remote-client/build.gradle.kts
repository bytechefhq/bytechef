dependencies {
    implementation("org.jspecify:jspecify")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-api"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-webmvc")
    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation("tools.jackson.core:jackson-databind")
    testImplementation(project(":server:ee:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-remote-rest"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
}
