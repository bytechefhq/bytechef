dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-relational")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:data-storage:data-storage-api"))
    implementation(project(":server:libs:core:data-storage:data-storage-db:data-storage-db-api"))

    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
