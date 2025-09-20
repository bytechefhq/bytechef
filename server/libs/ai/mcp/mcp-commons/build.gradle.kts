dependencies {
    api(project(":server:libs:platform:platform-api"))
    api("com.fasterxml.jackson.core:jackson-databind")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
}
