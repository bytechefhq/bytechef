dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")

    implementation("org.slf4j:slf4j-api")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-file-storage:hermes-file-storage-api"))
}
