dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-registry:platform-component-registry-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
    implementation(project(":server:ee:libs:core:discovery:discovery-util"))
}
