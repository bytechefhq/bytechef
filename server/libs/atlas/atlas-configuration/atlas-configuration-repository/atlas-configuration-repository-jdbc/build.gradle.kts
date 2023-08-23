dependencies {
    api(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-api"))

    implementation("org.springframework.data:spring-data-relational")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-jdbc-util"))
    testImplementation(project(":server:libs:configs:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
