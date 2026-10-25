# Microservices Plan 1: Config Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every EE microservice app carries its own configuration (no config server), `config-server-app` and `ai-copilot-app` are gone, and all eight apps still boot.

**Architecture:** A new resource-only module `server/ee/libs/config/microservice-config` holds the configuration shared by all apps (today's `config-server-app/.../config/apps/application*.yml`). Each app's `config/application.yml` keeps `spring.application.name` and imports, in order, `classpath:config/microservice.yml` then `classpath:config/<app>.yml`; Spring Boot gives later imports precedence and loads `-<profile>` variants of each import, which reproduces the config server's "app-specific beats shared, profile beats base" ordering with the same file split.

**Tech Stack:** Spring Boot 4 config data (`spring.config.import`), Gradle Kotlin DSL, JUnit 5, AssertJ.

**Spec:** `docs/superpowers/specs/2026-10-25-microservices-compose-design.md` (plan 1 of 4: config foundation and dead-app removal. Later plans: 2 gateway owns users and login, 3 gRPC transport, 4 images, Compose, docs, live verification.)

## Global Constraints

- Branch `microservices`, worktree `/Volumes/Data/bytechef/bytechef-microservices`, based on `upstream/6616`.
- Commit subjects only, no body, no ticket number for now, no `Co-Authored-By` line.
- All files under `server/ee/` use the ByteChef Enterprise license header; Java classes there carry `@version ee`.
- One shared PostgreSQL database for now: every `bytechef.datasource.url` dev default points at `jdbc:postgresql://localhost:5432/bytechef`.
- Only app modules are deleted; libraries they used (for example `server/ee/libs/ai/ai-copilot/*`) stay.
- Out of scope here: gateway routes, `session-config`, gRPC, images, Compose, `distributed.mdx` (plans 2 to 4).
- Run `./gradlew spotlessApply` before each commit; `check` on touched modules before the last commit.

## Review Focus

1. A key set in both the shared file and an app file must resolve to the app value; a reversed import order silently flips every override. Pinned by `MicroserviceConfigImportTest.testAppFileOverridesSharedFile`.
2. Profile files (`-dev`, `-prod`) must load for both imports, or dev ports and datasource URLs vanish without an error. Pinned by `MicroserviceConfigImportTest.testProfileVariantsLoadForBothImports`.
3. `#spring.profiles.active#` must still be substituted at build time in every app's `application.yml` after the rewrite. Pinned in Task 2 by checking `build/resources/main/config/application.yml`.
4. No app may still reference `configserver:` or `spring-cloud-starter-config`; a leftover optional import hides a missing file. Pinned in Task 3 by a grep that must return nothing.
5. The api-gateway route table (`apigateway-app.yml`) must survive the move byte for byte; plan 2 rewrites it, so losing it here breaks the diff plan 2 reviews. Pinned in Task 2 by `git diff -M` showing it as a rename.

---

### Task 1: Shared config module and worker-app migration

**Files:**
- Create: `server/ee/libs/config/microservice-config/build.gradle.kts`
- Create (git mv from `server/ee/apps/config-server-app/src/main/resources/config/apps/`): `server/ee/libs/config/microservice-config/src/main/resources/config/microservice.yml` (from `application.yml`), `microservice-dev.yml` (from `application-dev.yml`), `microservice-prod.yml` (from `application-prod.yml`)
- Create (test resources): `server/ee/libs/config/microservice-config/src/test/resources/config/test-app.yml`, `test-app-dev.yml`, and a key added via the test (see Step 1)
- Test: `server/ee/libs/config/microservice-config/src/test/java/com/bytechef/ee/microservice/config/MicroserviceConfigImportTest.java`
- Modify: `settings.gradle.kts` (add `include("server:ee:libs:config:microservice-config")` after `session-config`)
- Modify: `server/ee/apps/worker-app/src/main/resources/config/application.yml`
- Create (git mv): `server/ee/apps/worker-app/src/main/resources/config/worker-app.yml`, `worker-app-dev.yml`, `worker-app-prod.yml` (from config-server `apps/`)
- Modify: `server/ee/apps/worker-app/build.gradle.kts` (add `implementation(project(":server:ee:libs:config:microservice-config"))`, remove `spring-cloud-starter-config`)
- Test: `server/ee/apps/worker-app/src/test/java/.../WorkerApplicationIntTest.java`

**Interfaces:**
- Produces: classpath resources `config/microservice.yml`, `config/microservice-dev.yml`, `config/microservice-prod.yml`; the app `application.yml` shape below, which Task 2 copies.

App `config/application.yml` shape (worker shown; `#spring.profiles.active#` line kept as is):

```yaml
spring:
  application:
    name: worker-app
  config:
    import:
      - classpath:config/microservice.yml
      - classpath:config/worker-app.yml
  profiles:
    active: #spring.profiles.active#
```

- [ ] **Step 1: Write the failing test** `MicroserviceConfigImportTest` (plain JUnit, no Spring context class; build a minimal app with `new SpringApplicationBuilder(TestConfiguration.class).web(WebApplicationType.NONE)` where `TestConfiguration` is an empty `@Configuration` nested class, and pass `spring.config.import=classpath:config/microservice.yml,classpath:config/test-app.yml` plus `spring.config.name=none` so no `application.yml` is picked up). Test resources: `test-app.yml` sets `docs.documentation-url: https://app.example` and `test.marker: base`; `test-app-dev.yml` sets `test.marker: dev`.

```java
@Test
void testAppFileOverridesSharedFile() {
    // shared microservice.yml sets docs.documentation-url: https://docs.bytechef.io
    assertThat(environment().getProperty("docs.documentation-url")).isEqualTo("https://app.example");
}

@Test
void testSharedFileLoads() {
    assertThat(environment().getProperty("docs.report-bug-url"))
        .isEqualTo("https://github.com/bytechefhq/bytechef/issues");
}

@Test
void testProfileVariantsLoadForBothImports() {
    ConfigurableEnvironment environment = environment("dev");

    assertThat(environment.getProperty("test.marker")).isEqualTo("dev");
    // microservice-dev.yml sets logging.level.com.bytechef: DEBUG
    assertThat(environment.getProperty("logging.level.com.bytechef")).isEqualTo("DEBUG");
}
```

`environment(String... profiles)` is a private helper that runs the builder with those profiles, captures `context.getEnvironment()`, and closes the context.

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew :server:ee:libs:config:microservice-config:test --tests '*MicroserviceConfigImportTest'`
Expected: FAIL (module or resources missing).

- [ ] **Step 3: Create the module.** `build.gradle.kts` declares only `testImplementation("org.springframework.boot:spring-boot")` if the root build does not already supply it (check `server/ee/libs/config/cloud-config/build.gradle.kts` for the minimal shape). `git mv` the three shared files and rename them as listed. Add the `settings.gradle.kts` include.

- [ ] **Step 4: Run the test and confirm it passes**

Run: same as Step 2. Expected: 3 tests PASS. If `testProfileVariantsLoadForBothImports` fails, profile variants are not resolved for file imports in this Boot version: switch the shared and app files to multi-document YAML with `spring.config.activate.on-profile` sections, update the File list accordingly, and keep the test.

- [ ] **Step 5: Migrate worker-app.** `git mv` `worker-app.yml`, `worker-app-dev.yml`, `worker-app-prod.yml` into `server/ee/apps/worker-app/src/main/resources/config/`; delete `worker-app-prod.yml` instead if it is empty (0 lines today). Rewrite `application.yml` to the shape above. Update `build.gradle.kts`.

- [ ] **Step 6: Extend `WorkerApplicationIntTest`** with an injected `Environment` and:

```java
@Test
public void testSharedAndAppConfigurationLoad() {
    assertThat(environment.getProperty("spring.task.execution.thread-name-prefix")).isEqualTo("worker-app-task-");
    assertThat(environment.getProperty("docs.documentation-url")).isEqualTo("https://docs.bytechef.io");
}
```

- [ ] **Step 7: Run worker-app integration tests**

Run: `./gradlew :server:ee:apps:worker-app:testIntegration`
Expected: PASS. Baseline (2026-10-25, before any change): all eight apps' `testIntegration` pass.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add settings.gradle.kts server/ee/libs/config/microservice-config server/ee/apps/worker-app server/ee/apps/config-server-app
git commit -m "Move shared microservice configuration into microservice-config and import it from worker-app"
```

### Task 2: Migrate the remaining apps

**Files (per app: api-gateway-app, configuration-app, connection-app, coordinator-app, execution-app, scheduler-app, webhook-app):**
- Modify: `server/ee/apps/<app>/src/main/resources/config/application.yml` (Task 1 shape)
- Create (git mv from config-server `apps/`): `config/<app>.yml`, `config/<app>-dev.yml`, `config/<app>-prod.yml` (skip empty prod files). The gateway's files are named `apigateway-app*.yml` on the config server; rename them to `api-gateway-app*.yml` and set `spring.application.name: api-gateway-app` so name and file agree. Check `server/ee/libs/core/discovery` and route `lb://` URIs for any reference to `apigateway-app` before renaming; if one exists, keep the old name instead.
- Modify: each `server/ee/apps/<app>/build.gradle.kts` (add `microservice-config`, remove `spring-cloud-starter-config`)
- Modify: `configuration-app-dev.yml`, `connection-app-dev.yml`, `execution-app-dev.yml`, `scheduler-app-dev.yml`: `bytechef.datasource.url` becomes `jdbc:postgresql://localhost:5432/bytechef`
- Remove from `configuration-app/src/main/resources/config/application.yml`: the commented `service-id: config-server-app` block
- Test: each app's `*ApplicationIntTest` gets the Task 1 Step 6 test with its own thread-name prefix (`<app>-task-`; check each `<app>.yml` for the exact value; the gateway has none, so assert `spring.cloud.gateway.mvc.routes[0].id` is `configuration-service` instead)

**Interfaces:**
- Consumes: `config/microservice.yml` from Task 1; the app `application.yml` shape from Task 1.

- [ ] **Step 1: Add the configuration test to all seven IntTests.**
- [ ] **Step 2: Run them and confirm the new tests fail**

Run: `./gradlew $(for a in api-gateway-app configuration-app connection-app coordinator-app execution-app scheduler-app webhook-app; do printf ':server:ee:apps:%s:testIntegration ' $a; done) --continue`
Expected: each new test FAILS (property null); `testContextLoads` results match the baseline.

- [ ] **Step 3: Move the files, rewrite each `application.yml`, update each `build.gradle.kts`, and set the shared-database URLs.**
- [ ] **Step 4: Run the same command; expected all new tests PASS and no baseline-passing test regresses.**
- [ ] **Step 5: Confirm the build-time profile substitution and the gateway route move**

Run: `./gradlew :server:ee:apps:configuration-app:processResources && grep -n 'active:' server/ee/apps/configuration-app/build/resources/main/config/application.yml`
Expected: `active:` followed by a real profile list, not `#spring.profiles.active#`.
Run: `git diff --cached -M --stat | grep api-gateway-app.yml`
Expected: `apigateway-app.yml => api-gateway-app.yml` shown as a pure rename with 0 changed lines.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/ee/apps server/ee/apps/config-server-app
git commit -m "Move per-app configuration out of config-server-app into each microservice app"
```

### Task 3: Delete config-server-app and ai-copilot-app

**Files:**
- Delete: `server/ee/apps/config-server-app/` (only `ConfigServerApplication.java`, `build.gradle.kts` and its own `config/application*.yml` remain after Task 2)
- Delete: `server/ee/apps/ai-copilot-app/`
- Modify: `settings.gradle.kts` (remove both `include` lines, 572 and 574 today)
- Modify: `CLAUDE.md` (remove the `ai-copilot-app/` and `config-server-app/` bullets under "Enterprise Edition (EE) Microservices")

- [ ] **Step 1: Confirm nothing else depends on them**

Run: `git grep -n "config-server-app\|ai-copilot-app\|configserver\|spring-cloud-starter-config\|CopilotApplication\|ConfigServerApplication" -- ':!docs/' ':!server/ee/apps/config-server-app' ':!server/ee/apps/ai-copilot-app'`
Expected: only the `settings.gradle.kts` and `CLAUDE.md` lines this task edits. Anything else gets fixed in this task.

- [ ] **Step 2: Delete, edit, and re-run the grep from Step 1 with the two app paths no longer excluded.**
Expected: matches only under `docs/` (plan 4 rewrites `distributed.mdx`).

- [ ] **Step 3: Verify the build still configures and the apps compile**

Run: `./gradlew $(for a in api-gateway-app configuration-app connection-app coordinator-app execution-app scheduler-app webhook-app worker-app; do printf ':server:ee:apps:%s:compileJava ' $a; done) :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A server/ee/apps/config-server-app server/ee/apps/ai-copilot-app settings.gradle.kts CLAUDE.md
git commit -m "Remove config-server-app and ai-copilot-app"
```

### Task 4: Full verification

- [ ] **Step 1: Run checks on every touched module**

Run: `./gradlew :server:ee:libs:config:microservice-config:check $(for a in api-gateway-app configuration-app connection-app coordinator-app execution-app scheduler-app webhook-app worker-app; do printf ':server:ee:apps:%s:check :server:ee:apps:%s:testIntegration ' $a $a; done) --continue`
Expected: BUILD SUCCESSFUL, or only failures that were already in the baseline (record them in the PR description).

- [ ] **Step 2: If Step 1 changed formatting or exposed a fix, commit it as its own commit with a subject naming what it fixes.**
