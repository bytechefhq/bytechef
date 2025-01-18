dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-api"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-file-storage:platform-custom-component-file-storage-api"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-loader"))
}
