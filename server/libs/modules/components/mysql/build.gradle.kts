version="1.0"

dependencies {
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    runtimeOnly("mysql:mysql-connector-java:8.0.31")

    testImplementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
