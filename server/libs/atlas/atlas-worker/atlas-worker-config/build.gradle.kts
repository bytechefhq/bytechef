dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-impl"))
    implementation(project(":server:libs:core:autoconfigure-annotations"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
