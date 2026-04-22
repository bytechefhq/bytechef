dependencies {
    api(project(":server:libs:automation:automation-workspace-file:automation-workspace-file-api"))
    api(project(":server:libs:core:file-storage:file-storage-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
