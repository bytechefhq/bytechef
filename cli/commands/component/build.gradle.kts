dependencies {
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation(libs.com.squareup.javapoet)
    implementation(libs.io.swagger.parser.v3.swagger.parser)
    implementation(project(":server:sdks:java:component-api"))

    testImplementation(project(":cli:cli-app"))
}
