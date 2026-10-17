dependencies {
    implementation(project(":spring-ai:spring-ag-ui:packages:core"))
    implementation(project(":spring-ai:spring-ag-ui:packages:server"))
    implementation(project(":spring-ai:spring-ag-ui:servers:spring"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-api"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-service"))
}
