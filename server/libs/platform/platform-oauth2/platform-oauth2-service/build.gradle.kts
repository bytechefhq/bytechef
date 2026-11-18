
dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-github-proxy-client"))
    implementation(project(":server:libs:platform:platform-oauth2:platform-oauth2-api"))

    testImplementation(project(":server:libs:test:test-support"))
}
