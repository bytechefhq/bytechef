dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.springframework:spring-webmvc")
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
