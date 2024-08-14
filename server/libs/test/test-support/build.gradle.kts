dependencies {
   implementation("org.apache.commons:commons-lang3")
   implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
   implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
   implementation("org.skyscreamer:jsonassert")

   implementation(project(":sdks:backend:java:component-api"))
   implementation(project(":server:libs:core:commons:commons-util"))
}
