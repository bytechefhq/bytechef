version="1.0"

dependencies {
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
