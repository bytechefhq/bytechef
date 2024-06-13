# ByteChef Server
This is the server-side repository for the ByteChef framework.
<br><br>
For details on setting up your development machine, please refer to the [Setup Guide](../CONTRIBUTING.md#server-side)

### Development mode

Initial users available:
- admin@localhost.com/admin
- user@localhost.com/user

### Some useful gradle tasks:

- `./gradlew test` - Executes unit tests
- `./gradlew testIntegration` - Executes integration tests
- `./gradlew checkstyleMain checkstyleTest` - Checks `checkstyle` rules for the source code and tests
- `./gradlew pmdMain pmdTest` - Checks `pmd` rules for the source code and tests
- `./gradlew spotbugsMain spotbugsTest` - Checks `spotbugs` rules for the source code and tests
- `./gradlew check` - Executes all above tasks
- `./gradlew compileTestJava` - Compiles the source code and tests
- `./gradlew spotlessApply` - Source formats the source code and tests
