dependencies {
    api(project(":server:libs:platform:platform-api"))
    api("tools.jackson.core:jackson-databind")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-impl"))
}
