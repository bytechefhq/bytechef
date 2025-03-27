dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.junit.jupiter:junit-jupiter")
    implementation("org.skyscreamer:jsonassert")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
