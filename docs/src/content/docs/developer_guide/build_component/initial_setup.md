---
title: "Initial Setup"
description: " "
---

To create new component, we will use `example` component as template.

1. Copy the example component from `server/libs/modules/components/example` into a new package `server/libs/modules/components/newcomponent`, where `newcomponent` is name of your new component.
2. Inside the new package, replace all occurrences of word `example` with `newcomponent`. Then, replace all occurrences of `Example` with `NewConnector`.

#### Setup Gradle

3. In file `bytechef/settings.gradle.kts`, add line: `include("server:libs:modules:components:newcomponent")`
4. In both files `bytechef/server/ee/apps/worker-app/build.gradle.kts` and `bytechef/server/apps/server-app/build.gradle.kts`, add line `implementation(project(":server:libs:modules:components:newcomponent"))`
5. Load gradle changes. After that IntelliJ should recognize your connector as a java module.
