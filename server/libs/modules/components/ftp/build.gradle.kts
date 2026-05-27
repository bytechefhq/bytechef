version="1.0"

dependencies {
    implementation("commons-net:commons-net:3.11.1")
    implementation("com.hierynomus:sshj:0.40.0")

    testImplementation(project(":server:libs:platform:platform-file-storage:platform-file-storage-api"))
    testImplementation(project(":server:libs:platform:platform-file-storage:platform-file-storage-impl"))
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.testcontainers:junit-jupiter")
}
