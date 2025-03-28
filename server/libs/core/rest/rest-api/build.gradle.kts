dependencies {
    api("org.springframework:spring-webmvc")
    api(libs.org.openapitools.jackson.databind.nullable)
    api(project(":server:libs:core:exception:exception-api"))

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-web")
}
