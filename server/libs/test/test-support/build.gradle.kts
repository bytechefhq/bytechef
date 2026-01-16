dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.junit.jupiter:junit-jupiter")
    implementation("org.skyscreamer:jsonassert")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
