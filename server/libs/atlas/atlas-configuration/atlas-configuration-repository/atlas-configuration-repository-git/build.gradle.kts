dependencies {
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-api"))

    implementation(libs.org.eclipse.jgit.org.eclipse.jgit)
    implementation("org.springframework:spring-core")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-impl"))
}
