version="1.0"

dependencies {
    api(project(":server:sdks:backend:java:definition-api"))

    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
