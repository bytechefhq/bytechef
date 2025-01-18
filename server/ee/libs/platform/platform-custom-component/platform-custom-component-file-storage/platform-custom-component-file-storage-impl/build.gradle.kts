dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:core:file-storage:file-storage-aws:file-storage-aws-api"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-file-storage:platform-custom-component-file-storage-api"))
}