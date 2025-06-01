dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:core:evaluator:evaluator-api"))
    api(project(":server:libs:core:exception:exception-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("org.springframework.data:spring-data-relational")
    implementation(project(":server:libs:core:commons:commons-util"))
}
