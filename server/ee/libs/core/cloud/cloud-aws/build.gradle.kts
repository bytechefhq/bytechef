dependencies {
    implementation(libs.software.amazon.awssdk.auth)
    implementation(libs.software.amazon.awssdk.regions)
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
}
