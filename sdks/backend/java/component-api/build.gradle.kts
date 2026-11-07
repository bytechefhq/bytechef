version="1.0"

dependencies {
    api("org.jspecify:jspecify")
    api(project(":sdks:backend:java:definition-api"))

    implementation("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.skyscreamer:jsonassert")
    testImplementation("tools.jackson.core:jackson-databind")
}
