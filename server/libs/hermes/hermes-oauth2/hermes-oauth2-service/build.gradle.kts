dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:hermes:hermes-oauth2:hermes-oauth2-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
