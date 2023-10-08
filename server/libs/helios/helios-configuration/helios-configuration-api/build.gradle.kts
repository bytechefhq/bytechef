dependencies {
    api(project(":server:libs:core:category:category-api"))
    api(project(":server:libs:core:tag:tag-api"))
    api(project(":server:libs:hermes:hermes-configuration:hermes-configuration-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-connection:hermes-connection-api"))
}
