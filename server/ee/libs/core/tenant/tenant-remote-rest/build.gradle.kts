dependencies {
    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
