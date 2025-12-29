version="1.0"

dependencies {
    api("org.jspecify:jspecify")
    api(project(":sdks:backend:java:definition-api"))

    testImplementation("org.skyscreamer:jsonassert")
    testImplementation("tools.jackson.core:jackson-databind")
}
