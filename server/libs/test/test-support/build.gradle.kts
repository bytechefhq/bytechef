dependencies {
    api(project(":sdks:backend:java:component-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.skyscreamer:jsonassert")
}
