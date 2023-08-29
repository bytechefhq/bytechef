dependencies {
    implementation(project(":server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-api"))

    implementation(project(":server:ee:libs:core:commons:commons-webclient"))

    implementation("io.projectreactor:reactor-core")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:core:commons:commons-discovery"))
}
