dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    api("com.jayway.jsonpath:json-path")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-core")
}
