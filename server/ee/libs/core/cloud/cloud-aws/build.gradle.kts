dependencies {
    implementation("software.amazon.awssdk:auth")
    implementation("software.amazon.awssdk:regions")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
}
