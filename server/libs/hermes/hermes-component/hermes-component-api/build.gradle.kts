version="1.0"

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")
    api(project(":server:libs:hermes:hermes-definition-api"))

    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
