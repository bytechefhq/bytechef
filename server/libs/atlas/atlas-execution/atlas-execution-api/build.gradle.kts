dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    api(project(":server:libs:core:error-api"))
    api(project(":server:libs:core:file-storage:file-storage-api"))
    api(project(":server:libs:core:message:message-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator"))
}
