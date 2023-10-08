dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:dione:dione-configuration:dione-configuration-api"))

    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-service"))
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-jdbc"))
    testImplementation(project(":server:libs:core:category:category-service"))
    testImplementation(project(":server:libs:core:liquibase-config"))
    testImplementation(project(":server:libs:core:tag:tag-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
