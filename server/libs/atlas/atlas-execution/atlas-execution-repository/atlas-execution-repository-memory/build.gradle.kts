dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")
    api(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-api"))

    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))
}
