version="1.0"

dependencies {
    runtimeOnly("mysql:mysql-connector-java:8.0.31")

    testImplementation(project(":server:libs:hermes:hermes-component:hermes-component-impl"))
}
