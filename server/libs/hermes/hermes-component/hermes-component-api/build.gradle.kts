version="1.0"

dependencies {
    api(project(":server:libs:hermes:hermes-definition-api"))

    implementation("org.slf4j:slf4j-api")

    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
