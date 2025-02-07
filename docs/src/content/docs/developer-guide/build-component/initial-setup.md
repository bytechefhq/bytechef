---
title: "Initial Setup"
---

To create new component, we will use `example` component as template.

1. Copy the example component from `server/libs/modules/components/example` into a new package `server/libs/modules/components/newcomponent`, where `newcomponent` is name of your new component.

2. Update Settings:
    - Open `bytechef/settings.gradle.kts`.
    - Add the following line to include your new component in the build process:
      ```kotlin
      include("server:libs:modules:components:newcomponent")
      ```

3. Modify Build Files:
    - Open `bytechef/server/ee/apps/worker-app/build.gradle.kts` and `bytechef/server/apps/server-app/build.gradle.kts`.
    - Add the following line to both files to ensure your component is included as a dependency:
      ```kotlin    
      implementation(project(":server:libs:modules:components:newcomponent"))
      ```
4. Load Gradle Changes:
    - Refresh or reload the Gradle project in IntelliJ IDEA.
    - This step ensures that IntelliJ recognizes your new component as a Java module, allowing you to work with it seamlessly within the IDE.

5. Rename Package and Classes:
    - Inside the newly created package, rename the `example` subpackage to `newcomponent`.
    - Additionally, rename all classes within this package that start with `Example` to start with `NewComponent`.
