![anl-srv-readme-md](https://static.scarf.sh/a.png?x-pxid=a816af3f-ff0c-4038-b16e-1e0e2cabafac)
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
- `./gradlew generateOpenAPI` - Generates OpenAPI server models and API interfaces and client models and API implementations. Note: if during regeneration of existing specification models are not updated, they need to be deleted and task called again.
- `./gradlew generateDocumentation` - Generates documentation of every component.

### Database upgrade process
Upgrade database process uses [Liquibase](https://www.liquibase.com) as the engine. The upgrade liquibase files go under `src/main/resources/config/liquibase/changelog/[Module specific subpaths]` directory.

In order to define database schema changes for particular module follow the next steps:

1. Create new liquibase file with the following format:
 `[timestamp]_[module_name]_[short operation name].xml`
2. Use the following template as starting point:
```
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd"
>
    <changeSet id="[timestamp]" author="[Author's full name]" contextFilter="[Optional context filter name, depends on module and usage context]">
         [Liquibase Change Types]
    </changeSet>
</databaseChangeLog>
```

Check `libs/platform/platform-user/platform-user-service/src/main/resources/config/liquibase/changelog/platform/user` as an example.

For details check [Liquibase documentation](https://docs.liquibase.com/change-types/home.html).
