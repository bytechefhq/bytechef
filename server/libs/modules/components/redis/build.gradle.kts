version = "1.0"

dependencies {
    implementation("redis.clients:jedis:5.2.0")

    testImplementation(project(":server:libs:platform:platform-component:platform-component-test-int-support"))
}
