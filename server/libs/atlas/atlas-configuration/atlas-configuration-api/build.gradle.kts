dependencies {
    api("org.springframework.data:spring-data-commons")

    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("org.springframework.data:spring-data-relational")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator"))
}
