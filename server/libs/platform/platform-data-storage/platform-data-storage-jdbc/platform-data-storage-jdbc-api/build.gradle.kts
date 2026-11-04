dependencies {
    api(project(":server:libs:platform:platform-api"))
    api(project(":server:libs:platform:platform-data-storage:platform-data-storage-api"))

    compileOnly("com.github.spotbugs:spotbugs-annotations")

    implementation("org.jspecify:jspecify")
}
