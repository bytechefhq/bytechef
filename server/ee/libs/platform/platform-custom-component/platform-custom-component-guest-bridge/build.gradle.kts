dependencies {
    api(project(":sdks:backend:java:component-api"))

    compileOnly(rootProject.libs.org.graalvm.espresso.polyglot)

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
