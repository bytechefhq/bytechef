dependencies {
    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-notification:platform-notification-api"))
}
