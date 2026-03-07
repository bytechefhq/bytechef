version="1.0"

dependencies {
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    runtimeOnly(libs.com.oracle.database.jdbc.ojdbc11)

    testImplementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
