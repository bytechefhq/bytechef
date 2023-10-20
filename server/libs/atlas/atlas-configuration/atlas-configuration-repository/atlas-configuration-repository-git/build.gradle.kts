dependencies {
    implementation(libs.org.eclipse.jgit.org.eclipse.jgit)
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-api"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-impl"))
}
