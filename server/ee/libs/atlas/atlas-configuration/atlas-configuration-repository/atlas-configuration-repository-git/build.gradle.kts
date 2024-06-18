dependencies {
    api(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation(libs.org.eclipse.jgit.org.eclipse.jgit)
    implementation("org.springframework:spring-core")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:annotation-api"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-impl"))
}
