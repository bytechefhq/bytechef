dependencies {
    implementation("org.springframework.data:spring-data-relational")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-api"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-converter"))
    testImplementation(project(":server:libs:core:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
