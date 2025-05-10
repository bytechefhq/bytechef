---
title: "Initial Setup"
---

This section provides a clear step-by-step guide for setting up a new component in your project, ensuring it is properly integrated into the build system and recognized by the IDE.

1. Create a New Package:
    - Navigate to `server/apps/libs/modules/components/`.
    - Create a new package with the name of your component, e.g., `newcomponent`.

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
