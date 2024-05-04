version="1.0"

dependencies {
    api(project(":sdks:backend:java:definition-api"))

    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("org.skyscreamer:jsonassert")
}
