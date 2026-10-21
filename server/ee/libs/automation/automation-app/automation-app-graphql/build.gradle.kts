dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:ee:libs:automation:automation-app:automation-app-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")

    // AppFacadeAuthorizationTest, AppWorkflowFacadeAuthorizationTest and AppApiCollectionFacadeAuthorizationTest run
    // the REAL app facade guards through the EE PermissionServiceImpl. They live here, not in automation-app-service or
    // automation-configuration-service, because this module has no Liquibase-backed IntTests: putting the
    // configuration services on the app service's test classpath (or the app service on theirs) adds changelogs that
    // reference tables the other side's IntTests never create.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-config")
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-api"))
    testImplementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    testImplementation(project(":server:libs:platform:platform-user:platform-user-api"))
    testImplementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-api"))
    testImplementation(project(":server:ee:libs:automation:automation-app:automation-app-service"))
    testImplementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-api"))
    testImplementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:ee:libs:platform:platform-resource-grant:platform-resource-grant-api"))
}
