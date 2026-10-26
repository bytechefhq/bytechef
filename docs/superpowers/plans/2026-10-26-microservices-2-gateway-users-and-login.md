# Microservices Plan 2: Gateway Owns Users and Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The gateway becomes the front door: it routes every browser path to the right app, owns users, interactive login and the user GraphQL, stores the session in Redis, and the browser-facing backend apps authenticate requests from that same session.

**Architecture:** `api-gateway-app` gets a datasource (Liquibase context `user`), the user modules, `security-config` with form login and remember-me, and `session-config` with `bytechef.session.store=redis`. A gateway-only security chain lets proxied paths through untouched, so backend apps keep doing their own authorization, CSRF and per-request token auth. Backend apps (configuration, execution, webhook) get `security-config` with a new switch `bytechef.security.login.enabled=false`, so they read the shared session but never log anyone in. The client sends user GraphQL operations to `/graphql` with a header the gateway routes to its own GraphQL endpoint; the monolith ignores the header.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Cloud Gateway Server WebMVC 5.0.3 (`spring-cloud-dependencies` 2025.1.3), Spring Security 7, Spring Session Data Redis, Spring for GraphQL, Testcontainers (PostgreSQL, Redis), React 19 + Vitest.

**Spec:** `docs/superpowers/specs/2026-10-25-microservices-compose-design.md`

## Global Constraints

- All work is EE (`bytechef.edition=ee`); files under `server/ee/` use the Enterprise license header and Java classes get `@version ee`.
- No change in behavior of the monolith (`server-app`) or the #6616 cluster: every new switch defaults to today's behavior there.
- Login stays session based (Spring Session, Redis store via `session-config`); no JWT for the browser.
- Per-request token auth (API keys, embedded connected users, MCP OAuth) stays on the service that receives the request.
- One shared database (`jdbc:postgresql://localhost:5432/bytechef` in dev).
- Commit messages are subject only, no ticket number, no body, no trailers. Commit by path. Never push.
- No em dashes in user-facing prose. Java formatting by `./gradlew spotlessApply`; client by `npm run format`.
- `bytechef.coordinator.enabled` must not be `false` in the gateway: `AccountController`, `UserGraphQlController`, `UserManagementFacadeImpl` and `AuthorityGraphQlController` are `@ConditionalOnCoordinator`, and `session-config` skips when it is `false`.

## Out of scope (later plans)

- configuration-app's `UserService` calls (`CurrentUserResolver`, `ApiKeyFacadeImpl`, `SigningKeyFacadeImpl`) still hit the `RemoteUserServiceClient` stub after this plan. The user service is the first service ported in plan 3 (gRPC foundation), because it is the smallest one and every permission check needs it. Until then EE `@permissionService` checks in configuration-app fail; milestone 1 is verified in plan 4, after plan 3.
- Multi-tenant login is wired (both tenant security modules are on the gateway) but verified in a follow-up.
- Twilio media-stream WebSocket proxying.
- Mailpit and configuration-app's broken `spring.mail` block (plan 4).

## Review Focus

1. A remember-me cookie with an expired session: the first request after reload is `GET /api/account` (gateway), which must log the user back in and write a fresh session the backends then accept. Pinned in Task 4 (`testRememberMeRestoresSessionReadableFromRedis`).
2. An API-key or embedded-token request (no session, no CSRF cookie) to a proxied path must not be rejected by the gateway. Pinned in Task 4 (`testProxiedPathIsNotAuthenticatedByGateway`).
3. A user GraphQL mutation without the CSRF header must be rejected even though it arrives at `/graphql`, a path the gateway otherwise proxies. Pinned in Task 5 (`testUserGraphQlMutationWithoutCsrfTokenIsForbidden`).
4. A session written after social or SSO-OIDC login must survive Redis serialization. Pinned in Task 2.
5. A new user `.graphql` operation added to the client later must still go to the gateway. Pinned in Task 7 (the fetcher test enumerates the folder).

---

### Task 1: Precise gateway route table

The current routes are bound to `spring.cloud.gateway.mvc.routes`, which gateway 5.0.3 ignores (the bound prefix is `spring.cloud.gateway.server.webmvc.routes`), and `/api/automation/**` would send everything to configuration-app anyway. Routes are matched first to last; local gateway controllers only see paths no route matches.

**Files:**
- Modify: `server/ee/apps/api-gateway-app/src/main/resources/config/api-gateway-app.yml` (routes block; also rename the misplaced `spring.mail.base-url/from` to `bytechef.mail.base-url/from`)
- Create: `server/ee/apps/api-gateway-app/src/test/java/com/bytechef/api/gateway/config/GatewayRouteTableTest.java`

**Interfaces:**
- Produces: route ids `user-graphql`, `configuration-webhook-test`, `execution-app`, `webhook-app`, `configuration-app`, in that order, under `spring.cloud.gateway.server.webmvc.routes`. Task 4 derives the proxy security matcher from these routes' `Path` predicates; Task 5 relies on `user-graphql` being first.

- [ ] **Step 1: Write the failing test**

`GatewayRouteTableTest` binds `api-gateway-app.yml` into `GatewayMvcProperties` (use `Binder` over a `YamlPropertySourceLoader` source, no Spring context) and resolves a path to the first route whose `Path` predicate matches (`PathPatternParser.defaultInstance`), honoring a `Header` predicate when present. `@ParameterizedTest @CsvSource` table, path → expected route id:

```
/api/automation/internal/projects/1,                                   configuration-app
/api/automation/internal/workspaces/1/workflow-executions,              execution-app
/api/automation/internal/workspaces/1/projects,                         configuration-app
/api/automation/internal/workflow-executions/5,                         execution-app
/api/platform/internal/jobs/7,                                          execution-app
/api/platform/internal/trigger-executions,                              execution-app
/api/platform/internal/workflow-test-configurations/x,                  configuration-app
/api/embedded/internal/workflow-executions,                             execution-app
/api/embedded/internal/integrations,                                    configuration-app
/api/embedded/v1/ext-user/tools,                                        execution-app
/api/embedded/v1/ext-user/components/c/versions/1/actions/a,            execution-app
/api/embedded/v1/ext-user/components/c/connections,                     configuration-app
/api/embedded/v1/ext-user/workflows/uuid,                               webhook-app
/api/embedded/v1/app-events/1,                                          webhook-app
/webhooks/abc,                                                          webhook-app
/webhooks/abc/test/environments/1,                                      configuration-webhook-test
/approvals/abc,                                                         webhook-app
/callback,                                                              webhook-app
/job/resume/abc,                                                        webhook-app
/file-entries/abc/content,                                              webhook-app
/icons/slack.svg,                                                       configuration-app
/graphql,                                                               configuration-app
```

plus a separate test `testUserGraphQlHeaderRoutesToGateway`: `/graphql` with header `X-ByteChef-GraphQL-Schema: user` → `user-graphql`; and `testGatewayOwnedPathsMatchNoRoute`: `/api/account`, `/api/authentication`, `/api/logout`, `/api/register`, `/api/activate`, `/api/user/graphql`, `/oauth2/authorization/google`, `/login/oauth2/code/google`, `/actuator/health` → no route.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:apps:api-gateway-app:test --tests '*GatewayRouteTableTest'`
Expected: FAIL (no routes bound under the webmvc prefix).

- [ ] **Step 3: Rewrite the routes**

Routes under `spring.cloud.gateway.server.webmvc.routes`, `lb://<app>` URIs, in the order of the Interfaces block. `user-graphql` uses `uri: forward:/api/user/graphql` with predicates `Path=/graphql` and `Header=X-ByteChef-GraphQL-Schema, user`. Derive the remaining `Path` patterns from the table above and the controller base paths; most specific first. Drop `discovery.locator` (not a WebMVC property) and the connection-app route (connection-app serves only `/remote/**`).

- [ ] **Step 4: Run the test and the app IntTest**

Run: `./gradlew :server:ee:apps:api-gateway-app:test --tests '*GatewayRouteTableTest' :server:ee:apps:api-gateway-app:testIntegration`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/apps/api-gateway-app/src/main/resources/config/api-gateway-app.yml server/ee/apps/api-gateway-app/src/test/java/com/bytechef/api/gateway/config/GatewayRouteTableTest.java
git commit -m "Route each browser path to the microservice that serves it"
```

---

### Task 2: Social and SSO-OIDC principals survive session serialization

Spring Session uses JDK serialization; `CustomOAuth2User` and `CustomOidcUser` do not implement `Serializable` (and neither do `OAuth2User`/`OidcUser`), so writing an `OAuth2AuthenticationToken` holding them to Redis fails. This also affects the #6616 monolith with `bytechef.session.store=redis`.

**Files:**
- Modify: `server/libs/config/security-config/src/main/java/com/bytechef/security/web/oauth2/CustomOAuth2User.java`, `CustomOidcUser.java`
- Create: `server/libs/config/security-config/src/test/java/com/bytechef/security/web/oauth2/CustomOAuth2UserTest.java`, `CustomOidcUserTest.java`

- [ ] **Step 1: Write the failing tests**

`testSerializationRoundTrip` in each: build the principal with real values (authorities `ROLE_USER`, a name attribute, for OIDC an `OidcIdToken` with `sub`), wrap it in `new OAuth2AuthenticationToken(principal, authorities, "google")`, round-trip through `SerializationUtils.serialize` / `deserialize` (`org.springframework.util`), and assert the deserialized principal's `getName()` and `getAuthorities()` equal the original's.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:config:security-config:test --tests '*CustomO*UserTest'`
Expected: FAIL with `NotSerializableException`.

- [ ] **Step 3: Implement `Serializable` on both classes** with a `serialVersionUID`; any non-serializable field becomes serializable data (for example a plain `Map<String, Object>` of attributes) rather than `transient`, so nothing is lost on read.

- [ ] **Step 4: Run them to verify they pass**

Same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/libs/config/security-config/src/main/java/com/bytechef/security/web/oauth2/CustomOAuth2User.java server/libs/config/security-config/src/main/java/com/bytechef/security/web/oauth2/CustomOidcUser.java server/libs/config/security-config/src/test/java/com/bytechef/security/web/oauth2/
git commit -m "Make social and SSO OIDC principals serializable for the shared session store"
```

---

### Task 3: Switch to turn interactive login off in an app

Backend apps need `SecurityConfiguration` to read the session, authorize and run per-request token contributors, but must not log anyone in: their remember-me would call `PersistentTokenService` and `UserService`, which they do not have.

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (`Security.Login` with `boolean enabled`, no initializer)
- Modify: `server/libs/config/security-config/src/main/java/com/bytechef/security/config/SecurityConfiguration.java`
- Modify: `server/libs/config/security-config/src/main/java/com/bytechef/security/web/rememberme/PersistentTokenRememberMeServices.java`
- Modify: `server/libs/config/tenant-single-security-config/src/main/java/com/bytechef/tenant/single/security/SingleTenantUserDetailsService.java` and the two `SingleTenantAuthentication*Handler` classes; the matching `MultiTenant*` classes in `server/ee/libs/config/tenant-multi-security-config`
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml` (`bytechef.security.login.enabled: true`), `server/ee/libs/config/microservice-config/src/main/resources/config/microservice.yml` (`false`), and any test `application-testint.yml` whose tests log in
- Create: `server/libs/config/security-config/src/test/java/com/bytechef/security/config/SecurityConfigurationIntTest.java` (or add `@Nested` classes if it exists)

**Interfaces:**
- Produces: property `bytechef.security.login.enabled` (`ApplicationProperties.Security.Login#isEnabled()`). When `true`, `SecurityConfiguration.apiFilterChain` is unchanged. When `false`, it applies no form login, remember-me, logout, OAuth2 or SAML2 customizers, and its constructor needs no `AuthenticationSuccessHandler`, `AuthenticationFailureHandler` or `RememberMeServices` (take them as `ObjectProvider`). The beans that need user services are `@ConditionalOnProperty(prefix = "bytechef.security.login", name = "enabled", havingValue = "true")`.

- [ ] **Step 1: Write the failing tests**

A `@WebMvcTest`-style IntTest booting `SecurityConfiguration` with a stub `@RestController` at `/api/ping`, with **no** `UserService`, `PersistentTokenService` or `UserDetailsService` beans and `bytechef.security.login.enabled=false`:
- `testContextStartsWithoutUserServices`: the context loads.
- `testLoginEndpointIsNotServed`: `POST /api/authentication` with CSRF, `username=admin&password=admin` → `401`.
- `testRememberMeCookieIsIgnored`: `GET /api/ping` with cookie `remember-me=abc` → `401`, no `Set-Cookie: remember-me`.
- `testSessionAuthenticationIsHonored`: `GET /api/ping` with `.with(user("admin"))` → `200`.

A second `@Nested` class with `enabled=true` and mocked user services: `testLoginEndpointIsServed`: `POST /api/authentication` reaches the success/failure handler (status not `401` from the entry point; assert the handler's status).

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:config:security-config:testIntegration --tests '*SecurityConfigurationIntTest'`
Expected: FAIL (context fails to start: missing `RememberMeServices`).

- [ ] **Step 3: Implement the switch** as in the Interfaces block. Set the yml defaults listed under Files.

- [ ] **Step 4: Run the touched modules' checks**

Run: `./gradlew :server:libs:config:security-config:check :server:libs:config:tenant-single-security-config:check :server:ee:libs:config:tenant-multi-security-config:check :server:ee:libs:config:security-sso-config:check :server:apps:server-app:compileTestJava`
Expected: PASS. A test that logs in and now fails gets `bytechef.security.login.enabled: true` in its testint yml.

- [ ] **Step 5: Commit**

```bash
git add <the files above>
git commit -m "Add a switch to turn interactive login off in apps that only read the session"
```

---

### Task 4: Gateway hosts users, login and the shared session

**Files:**
- Modify: `server/ee/apps/api-gateway-app/build.gradle.kts`
- Modify: `server/ee/apps/api-gateway-app/src/main/resources/config/api-gateway-app.yml`, `api-gateway-app-dev.yml`, `src/test/resources/config/application-testint.yml`
- Create: `server/ee/apps/api-gateway-app/src/main/java/com/bytechef/api/gateway/security/GatewayProxySecurityConfiguration.java`
- Create: `server/ee/apps/api-gateway-app/src/main/java/com/bytechef/api/gateway/security/GatewayRoutedPaths.java`
- Create: `server/ee/apps/api-gateway-app/src/test/java/com/bytechef/api/gateway/ApiGatewayLoginIntTest.java`

**Interfaces:**
- Consumes: Task 1 routes; Task 3 `bytechef.security.login.enabled` (`true` here).
- Produces: `GatewayRoutedPaths` (`@Component`) with `List<String> getPathPatterns()` (every `Path` pattern of every non-`forward:` route) and `List<String> getNonSpaPathPrefixes()` (each pattern's literal prefix before the first wildcard segment, deduplicated). It implements `SpaWebFilterContributor`. `GatewayProxySecurityConfiguration` declares `@Bean @Order(0) SecurityFilterChain proxyFilterChain` matching those patterns **and** not carrying header `X-ByteChef-GraphQL-Schema`, with `permitAll`, CSRF off, `SessionCreationPolicy.NEVER`, request cache off.

Dependencies to add (as `server-app` declares them): `spring-boot-starter-security`, `spring-boot-starter-mail`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-cache`, `jdbc-config`, `liquibase-config`, `cache-config`, `security-config`, `tenant-single-security-config`, EE `tenant-multi-security-config`, EE `security-sso-config`, EE `session-config`, `platform-mail`, `platform-user-service`, `platform-user-rest`, EE `platform-user-service`, `static-resources-config`, PostgreSQL and HikariCP at runtime; `tenant-multi-data-config` like configuration-app. Config: datasource `${bytechef.datasource.*}`, `spring.liquibase.contexts: user`, `bytechef.security.login.enabled: true`, `bytechef.session.store: redis`, `bytechef.resources.web` for the static client.

- [ ] **Step 1: Write the failing tests**

`ApiGatewayLoginIntTest`: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("testint")`, Postgres via `PostgreSQLContainerConfiguration` and Redis via `RedisContainerConfiguration` (`server/libs/test/test-int-support`). Create a user `login-test@localhost.com` / `secret` with role `ROLE_USER` through `UserService` in `@BeforeEach`. Use `TestRestTemplate` or `RestClient` against the port, carrying cookies.
- `testFormLoginWritesSessionToRedis`: `GET /api/account` to receive `XSRF-TOKEN`; `POST /api/authentication` (form, with `X-XSRF-TOKEN`) → `200` and a `SESSION` cookie; `RedisSessionRepository#findById(decoded SESSION)` holds a `SecurityContext` whose name is the login.
- `testAccountUsesTheSession`: after login, `GET /api/account` → `200`, body `login` equals the user.
- `testRememberMeRestoresSessionReadableFromRedis`: log in with `remember-me=true`, drop the `SESSION` cookie, `GET /api/account` with only `remember-me` → `200` and a new `SESSION` cookie whose Redis session holds the same principal.
- `testLogoutDeletesTheSession`: `POST /api/logout` → `200`; the Redis session is gone.
- `testProxiedPathIsNotAuthenticatedByGateway`: `POST /api/automation/internal/projects` with no session, no CSRF → status is neither `401` nor `403` (no backend runs, so expect `503`).
- `testSpaPathServesIndex` (with a temp `bytechef.resources.web` folder containing `index.html`, profile `prod` activated in a `@Nested` class): `GET /automation/projects` → `200` with the index body; `GET /webhooks/abc` → not the index body.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:apps:api-gateway-app:testIntegration --tests '*ApiGatewayLoginIntTest'`
Expected: FAIL (`/api/authentication` not served).

- [ ] **Step 3: Implement** the dependencies, config, `GatewayRoutedPaths` and `GatewayProxySecurityConfiguration` as in the Interfaces block.

- [ ] **Step 4: Run the gateway checks**

Run: `./gradlew :server:ee:apps:api-gateway-app:check :server:ee:apps:api-gateway-app:testIntegration`
Expected: PASS, including Task 1's route test and the plan 1 discovery test.

- [ ] **Step 5: Commit**

```bash
git add server/ee/apps/api-gateway-app
git commit -m "Host users, login and the Redis session in the API gateway"
```

---

### Task 5: Gateway serves the user GraphQL

**Files:**
- Modify: `server/ee/apps/api-gateway-app/build.gradle.kts` (`spring-boot-starter-graphql`, `graphql-impl`, `platform-user-graphql`, EE `platform-user-graphql`)
- Modify: `server/ee/apps/api-gateway-app/src/main/resources/config/api-gateway-app.yml` (`spring.graphql.http.path: /api/user/graphql`, `spring.graphql.schema.locations: classpath*:graphql/**/`)
- Create: `server/ee/apps/api-gateway-app/src/main/resources/graphql/gateway-base.graphqls` (root `type Query` and `type Mutation`, each with `_placeholder: Boolean`, as in `platform-configuration-graphql`'s `base.graphqls`)
- Create: `server/ee/apps/api-gateway-app/src/test/java/com/bytechef/api/gateway/ApiGatewayUserGraphQlIntTest.java`

**Interfaces:**
- Consumes: Task 1 route `user-graphql` (`/graphql` + header → `forward:/api/user/graphql`); Task 4 login.
- Produces: the gateway answers user, authority and identity provider operations at `/api/user/graphql`, and at `/graphql` when the request carries `X-ByteChef-GraphQL-Schema: user`.

- [ ] **Step 1: Write the failing tests** (same containers and login helper as Task 4; extract the helper into a test support class in the same package if both tests need it)
- `testUsersQueryThroughHeaderRoute`: logged-in admin, `POST /graphql` with the header, `X-XSRF-TOKEN`, body `{"query":"query users { users(pageNumber: 0, pageSize: 10) { content { login } } }"}` → `200`, `data.users.content[*].login` contains the admin.
- `testAuthoritiesQueryThroughHeaderRoute`: `query authorities { authorities }` → contains `ROLE_ADMIN`.
- `testUserGraphQlRequiresAuthentication`: no session → `401`.
- `testUserGraphQlMutationWithoutCsrfTokenIsForbidden`: logged in, `mutation { deleteUser(login: "x") }` with the header but no `X-XSRF-TOKEN` → `403`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:apps:api-gateway-app:testIntegration --tests '*ApiGatewayUserGraphQlIntTest'`
Expected: FAIL (forward target not found).

- [ ] **Step 3: Implement** the dependencies, config and root schema.

- [ ] **Step 4: Run the gateway checks**

Run: `./gradlew :server:ee:apps:api-gateway-app:check :server:ee:apps:api-gateway-app:testIntegration`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/apps/api-gateway-app
git commit -m "Serve the user GraphQL from the API gateway"
```

---

### Task 6: Browser-facing backend apps authenticate from the shared session

configuration-app, execution-app and webhook-app serve browser and token-authenticated paths but have no Spring Security today, so every path is open. connection-app and scheduler-app serve only `/remote/**` and get nothing here.

**Files:**
- Modify: `server/ee/apps/{configuration-app,execution-app,webhook-app}/build.gradle.kts`: `spring-boot-starter-security`, `security-config`, EE `session-config`, and the per-request auth modules the monolith uses for the paths each app serves: `embedded-security-web-impl` (all three serve `/api/embedded/v1/**`), CE and EE `platform-security-web-impl`. A module that cannot boot in an app without beans the app lacks is left out with a ledger ruling naming the bean.
- Modify: each app's `config/<app>.yml`: `bytechef.session.store: redis` (`bytechef.security.login.enabled: false` comes from `microservice.yml`)
- Modify: each app's `src/test/resources/config/application-testint.yml` and `*ApplicationIntTest.java` (add `RedisContainerConfiguration`)
- Create: `server/ee/apps/{configuration-app,execution-app,webhook-app}/src/test/java/.../<App>SessionIntTest.java`

**Interfaces:**
- Consumes: Task 3 switch (`false` from `microservice.yml`); `session-config` Redis store; Task 2 serializable principals.

- [ ] **Step 1: Write the failing tests**, one per app, Redis container plus whatever the app's IntTest already uses:
- `testUnauthenticatedRequestIsRejected`: `GET` a protected path the app serves (configuration-app `/api/automation/internal/projects`, execution-app `/api/platform/internal/jobs`, webhook-app: `GET /api/embedded/v1/app-events` without a token) → `401`.
- `testSessionWrittenElsewhereIsAccepted` (configuration-app and execution-app): build a `SecurityContextImpl` with `UsernamePasswordAuthenticationToken(new User("admin@localhost.com", "", authorities ROLE_ADMIN), null, authorities)`, save it with the app's `RedisSessionRepository` under `SPRING_SECURITY_CONTEXT`, send its id Base64-encoded as cookie `SESSION` → status is not `401`.
- `testPublicWebhookStaysOpen` (webhook-app): `POST /webhooks/unknown-id` with no session → not `401`/`403`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:apps:configuration-app:testIntegration :server:ee:apps:execution-app:testIntegration :server:ee:apps:webhook-app:testIntegration --tests '*SessionIntTest'`
Expected: FAIL (`200` for the unauthenticated request).

- [ ] **Step 3: Implement** the dependencies and config.

- [ ] **Step 4: Run all EE app checks**

Run: `./gradlew :server:ee:apps:configuration-app:check :server:ee:apps:execution-app:check :server:ee:apps:webhook-app:check :server:ee:apps:configuration-app:testIntegration :server:ee:apps:execution-app:testIntegration :server:ee:apps:webhook-app:testIntegration`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/apps/configuration-app server/ee/apps/execution-app server/ee/apps/webhook-app
git commit -m "Authenticate browser requests in the microservices from the shared Redis session"
```

---

### Task 7: Client sends user GraphQL operations to the gateway

**Files:**
- Modify: `client/src/shared/middleware/graphqlFetcher.ts`
- Create: `client/src/shared/middleware/tests/graphqlFetcher.test.ts`

**Interfaces:**
- Produces: `USER_GRAPHQL_OPERATION_NAMES: ReadonlySet<string>` (exported) with `authorities`, `createIdentityProvider`, `deleteIdentityProvider`, `deleteUser`, `identityProvider`, `identityProviders`, `inviteUser`, `updateIdentityProvider`, `updateUser`, `users`. `fetcher` reads the operation name from the query text (`/^\s*(query|mutation)\s+(\w+)/`) and adds header `'X-ByteChef-GraphQL-Schema': 'user'` when the name is in the set. The URL stays `/graphql`.

- [ ] **Step 1: Write the failing tests**
- `sends the user schema header for user operations`: mock `fetch`, call `fetcher('query users { users { content { login } } }')()` → request headers contain `X-ByteChef-GraphQL-Schema: user`, URL `/graphql`.
- `does not send the header for other operations`: `query projects { ... }` → header absent.
- `lists every operation in src/graphql/platform/user`: `import.meta.glob('/src/graphql/platform/user/*.graphql', {eager: true, import: 'default', query: '?raw'})`, extract each operation name with the same regex, expect the set to equal `USER_GRAPHQL_OPERATION_NAMES`.

- [ ] **Step 2: Run them to verify they fail**

Run: `cd client && npx vitest run src/shared/middleware/tests/graphqlFetcher.test.ts`
Expected: FAIL (export missing).

- [ ] **Step 3: Implement** as in the Interfaces block (keys sorted, per the client sort-keys rule).

- [ ] **Step 4: Run the client checks**

Run: `cd client && npm run format && npm run check 2>&1 | tail -30`
Expected: lint, typecheck and tests pass; no `Unhandled` errors in the Vitest output.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/middleware/graphqlFetcher.ts client/src/shared/middleware/tests/graphqlFetcher.test.ts
git commit -m "client - Send user GraphQL operations with the user schema header"
```
