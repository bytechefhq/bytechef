dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))

    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-api"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-file-storage:platform-custom-component-file-storage-api"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-loader"))

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
