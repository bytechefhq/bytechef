dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.graphql:spring-graphql")
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:core:graphql:graphql-api"))
}
