dependencies {
    api("org.springframework.data:spring-data-commons")

    implementation("org.springframework.data:spring-data-relational")
    implementation(project(":server:libs:core:exception:exception-api"))

    testImplementation("org.assertj:assertj-core")
}
