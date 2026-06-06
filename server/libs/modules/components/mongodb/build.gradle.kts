version = "1.0"

dependencies {
    implementation("org.mongodb:mongodb-driver-sync")

    testImplementation(project(":server:libs:platform:platform-component:platform-component-test-int-support"))
}
