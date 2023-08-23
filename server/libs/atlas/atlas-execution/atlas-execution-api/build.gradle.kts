dependencies {
    api(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    api(project(":server:libs:core:error:error-api"))
    api(project(":server:libs:core:event:event-api"))

    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator"))
}
