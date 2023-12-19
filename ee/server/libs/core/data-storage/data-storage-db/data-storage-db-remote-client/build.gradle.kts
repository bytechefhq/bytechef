dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:data-storage:data-storage-api"))
    implementation(project(":server:libs:core:data-storage:data-storage-db:data-storage-db-api"))
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":ee:server:libs:core:commons:commons-rest-client"))
}
