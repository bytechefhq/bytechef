dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")
    api(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-api"))

    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))
}
