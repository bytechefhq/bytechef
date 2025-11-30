version="1.0"

dependencies {
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    runtimeOnly("com.oracle.database.jdbc:ojdbc11:23.3.0.23.09")

    testImplementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
