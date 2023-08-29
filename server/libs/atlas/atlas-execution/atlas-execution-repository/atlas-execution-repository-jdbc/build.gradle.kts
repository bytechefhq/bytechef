dependencies {
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-api"))

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-converter"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-converter"))
    testImplementation(project(":server:libs:configs:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
