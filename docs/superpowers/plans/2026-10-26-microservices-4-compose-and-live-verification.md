# Microservices Plan 4: Images, Docker Compose, Docs and Live Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** One `docker compose up` starts the EE microservice deployment, and the milestone 1 flow works end to end through the gateway.

**Architecture:** One shared Dockerfile builds an image per app from its boot jar (the gateway's image also carries the built React client). `docker-compose.microservices.yml`, modeled on `docker-compose.cluster.yml`, starts Postgres, Redis, RabbitMQ, Mailpit, a one-shot `migrate` service and the eight apps, configured only through environment variables, with only the gateway (and the Mailpit UI) published. A live run of the milestone 1 flow then finds the stub calls that matter; each is implemented over gRPC with a failing test first. The public distributed-installation page is rewritten for this layout.

**Tech Stack:** Docker / Docker Compose v2, GraalVM community 25 base image, Spring Boot 4.1.1, Gradle, React client build (`npm run build`), Playwright (or Chrome DevTools) MCP for the browser run.

**Spec:** `docs/superpowers/specs/2026-10-25-microservices-compose-design.md`. Patterns to copy: `docker-compose.cluster.yml`, `server/ee/apps/runtime-job-app/Dockerfile`, root `Dockerfile` (client copy), `server/docker-compose.dev.infra.yml` (Mailpit).

## Global Constraints

- Compose is built like `docker-compose.cluster.yml`: one shared environment anchor, required secrets fail fast with `${VAR:?...}`, topology chosen with variables and defaults (never by editing the file), health checks everywhere, a one-shot `migrate` service that runs Liquibase before any app starts.
- Services: `api-gateway-app`, `configuration-app`, `execution-app`, `webhook-app`, `coordinator-app`, `worker-app`, `connection-app`, `scheduler-app`, plus PostgreSQL, Redis, RabbitMQ and Mailpit (`axllent/mailpit`, SMTP 1025, UI 8025).
- Images: `BYTECHEF_<APP>_IMAGE` variables with `docker.bytechef.io/bytechef/<app>:latest` defaults (e.g. `BYTECHEF_API_GATEWAY_APP_IMAGE`), so a locally built image can be swapped in.
- Only the gateway port is published (`${BYTECHEF_PORT:-8080}`); the Mailpit UI (`${MAILPIT_UI_PORT:-8025}`) is published so sign-up mail can be read. Every other service is reachable only on the Compose network.
- Message broker `amqp` (RabbitMQ) by default, `redis` selectable with `BYTECHEF_MESSAGE_BROKER_PROVIDER`.
- Single tenant by default, `BYTECHEF_TENANT_MODE=multi` optional (verified in single only). Activation off: `bytechef.sign-up.activation-required=false`.
- `bytechef.remote.grpc.plaintext=true` (trusted network) and a required `BYTECHEF_REMOTE_GRPC_SERVICE_TOKEN`.
- No secrets served over HTTP; no change in behavior of the monolith or the #6616 cluster file.
- Commit messages subject only, no ticket number; commit by path; never push. No em dashes in docs or other user-facing prose.

## Decided here (Claude's calls, open to override)

- **`migrate` runs the monolith image's `liquibase` command**, exactly as `docker-compose.cluster.yml` does, and every app gets `SPRING_LIQUIBASE_ENABLED=false`. Reason: the apps' own contexts (`user`, `configuration`, `execution`, `connection`, `scheduler`) skip 28 changesets that carry only the `mono` context, so a schema built by the apps alone would miss tables; the monolith's migration runs every changelog once, before anything starts. This is the only place the monolith image appears; runtime services are the microservice images (decision 6).
- **Images are built by Compose** from `build:` sections (shared Dockerfile, `APP_NAME` build argument, `gateway` target adds the client), after `./gradlew bootJar` for the apps and `npm run build` in `client/`. No separate build script.
- **Compose file name:** `docker-compose.microservices.yml` at the repository root, next to `docker-compose.cluster.yml`.

## Review Focus

1. A browser session written by the gateway at login is honored by configuration-app's `/graphql` and execution-app's REST calls (the shared Redis session across containers). Pinned in Task 4 checklist items 1 and 4.
2. Discovery registers each container's own address and gRPC port; the gateway's `lb://` routes and the gRPC `discovery:///` targets reach the right container. Pinned in Task 3 (`docker compose up --wait` all healthy) and Task 4 item 1.
3. Webhook URLs shown in the editor point at the gateway's public URL, not an internal container address. Pinned in Task 4 item 5.
4. A scheduled trigger fires exactly once per tick with one scheduler and one coordinator. Pinned in Task 4 item 6.
5. Restarting one service (configuration-app) keeps the user logged in and the gateway routes to the new container. Pinned in Task 4 item 8.

---

### Task 1: Container-ready configuration

**Files:**
- Modify: `server/ee/apps/configuration-app/src/main/resources/config/application.yml` (remove the literal `spring.mail` block with host `http://127.0.0.1`, port 8080 and placeholder credentials)
- Modify: `server/ee/apps/configuration-app/src/main/resources/config/configuration-app.yml` (map `spring.mail.host/port/username/password` from `bytechef.mail.*` with the same defaults as `api-gateway-app.yml`)
- Modify: `CLAUDE.md` (Default Ports: `Mailpit: 1025 (SMTP), 8025 (UI)` instead of Mailhog)
- Test: `server/ee/apps/configuration-app/src/test/java/com/bytechef/configuration/ConfigurationApplicationIntTest.java`

- [ ] **Step 1: Write the failing test**

`testMailUsesBytechefMailSettings`: with `bytechef.mail.host=mailpit` and `bytechef.mail.port=1025` (add to the test's properties), `environment.getProperty("spring.mail.host")` is `mailpit` and `spring.mail.port` is `1025`.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:apps:configuration-app:testIntegration --tests '*ConfigurationApplicationIntTest'`
Expected: FAIL (`spring.mail.host` is `http://127.0.0.1`).

- [ ] **Step 3: Implement** the mapping and the CLAUDE.md line.

- [ ] **Step 4: Run the app's check**

Run: `./gradlew :server:ee:apps:configuration-app:check`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/apps/configuration-app CLAUDE.md
git commit -m "Take configuration-app mail settings from bytechef.mail like the gateway"
```

---

### Task 2: One image per app

**Files:**
- Create: `server/ee/apps/Dockerfile` (two targets: `app` copies `server/ee/apps/${APP_NAME}/build/libs/${APP_NAME}.jar` into `/opt/bytechef/app/app.jar` on `ghcr.io/graalvm/graalvm-community:25.0.2`, with `/opt/bytechef/app/tmp` and `/opt/bytechef/data/file-storage`, entrypoint `java -Dfile.encoding=UTF-8 -Duser.timezone=GMT -Djava.io.tmpdir=/opt/bytechef/app/tmp -jar app/app.jar` like `runtime-job-app`'s; `gateway` extends `app` and copies `client/dist` into `/opt/bytechef/client/` exactly as the root `Dockerfile` lists the files)
- Modify: `server/ee/apps/build.gradle.kts` only if the boot jar is not already named `<app>.jar` (set `archiveFileName` for the app subprojects)
- Create: `.dockerignore` entries if the build context would otherwise include `node_modules` or `.gradle` (check the existing root `.dockerignore` first)

**Interfaces:**
- Produces: build context = repository root; targets `app` and `gateway`; build argument `APP_NAME`. Task 3 consumes these in `build:` sections.

- [ ] **Step 1: Build the jars and the client**

Run: `./gradlew :server:ee:apps:api-gateway-app:bootJar :server:ee:apps:configuration-app:bootJar && (cd client && npm run build)`
Expected: `server/ee/apps/api-gateway-app/build/libs/api-gateway-app.jar` and `client/dist/index.html` exist (fix the jar name first if it differs).

- [ ] **Step 2: Verify the images do not exist yet / the build fails without the Dockerfile**

Run: `docker build -f server/ee/apps/Dockerfile --target gateway --build-arg APP_NAME=api-gateway-app -t bytechef-microservices/api-gateway-app:local .`
Expected: FAIL (no Dockerfile).

- [ ] **Step 3: Write the Dockerfile** as in Files.

- [ ] **Step 4: Build and inspect both targets**

Run: the build from Step 2, then `docker run --rm --entrypoint ls bytechef-microservices/api-gateway-app:local /opt/bytechef/app /opt/bytechef/client`, and the `app` target for `configuration-app` with `ls /opt/bytechef/app`.
Expected: `app.jar` in both; `index.html` and `assets` in the gateway image only.

- [ ] **Step 5: Commit**

```bash
git add server/ee/apps/Dockerfile server/ee/apps/build.gradle.kts .dockerignore
git commit -m "Build one image per microservice from a shared Dockerfile"
```

---

### Task 3: The Compose file

**Files:**
- Create: `docker-compose.microservices.yml`

**Interfaces:**
- Consumes: Task 2 targets.
- Produces: services `postgres`, `redis`, `rabbitmq`, `mailpit`, `migrate`, `api-gateway-app`, `configuration-app`, `connection-app`, `execution-app`, `webhook-app`, `coordinator-app`, `worker-app`, `scheduler-app`.

Content requirements:
- Anchor `x-bytechef-environment` with: `SPRING_PROFILES_ACTIVE: prod`, `BYTECHEF_EDITION: ee`, datasource (`jdbc:postgresql://postgres:5432/bytechef`, user `postgres`, `${POSTGRES_PASSWORD:?...}`), `SPRING_LIQUIBASE_ENABLED: "false"`, encryption (`property` provider, `${BYTECHEF_ENCRYPTION_KEY:?...}`), file storage `filesystem` at `/opt/bytechef/data/file-storage` (shared volume), message broker `${BYTECHEF_MESSAGE_BROKER_PROVIDER:-amqp}` with RabbitMQ host/port/user/`${RABBITMQ_PASSWORD:?...}`, Redis host `redis` port 6379 (set every property the apps read for Redis: Spring Data Redis, the Redis discovery client and the session store; confirm each from the apps' resolved environment), `BYTECHEF_PUBLIC_URL: ${BYTECHEF_PUBLIC_URL:-http://localhost:8080}`, `BYTECHEF_SESSION_STORE: redis`, `BYTECHEF_TENANT_MODE: ${BYTECHEF_TENANT_MODE:-single}`, `BYTECHEF_SIGN_UP_ACTIVATION_REQUIRED: "false"`, `BYTECHEF_MAIL_HOST: mailpit`, `BYTECHEF_MAIL_PORT: 1025`, `BYTECHEF_REMOTE_GRPC_PLAINTEXT: "true"`, `BYTECHEF_REMOTE_GRPC_SERVICE_TOKEN: ${BYTECHEF_REMOTE_GRPC_SERVICE_TOKEN:?...}`, `BYTECHEF_SECURITY_REMEMBER_ME_KEY: ${BYTECHEF_SECURITY_REMEMBER_ME_KEY:?...}`, `SERVER_PORT: 8080`, `JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=75`.
- Anchor `x-bytechef-app` with `restart: unless-stopped`, `depends_on` migrate (`service_completed_successfully`), postgres, redis, rabbitmq (`service_healthy`), healthcheck `curl -fs http://localhost:8080/actuator/health/readiness` (as the cluster file), file-storage volume.
- Each app: `image: ${BYTECHEF_<APP>_IMAGE:-docker.bytechef.io/bytechef/<app>:latest}`, `build:` (context `.`, dockerfile `server/ee/apps/Dockerfile`, target `app` or `gateway`, `args: {APP_NAME: <app>}`), `environment: *bytechef-environment` plus `SPRING_APPLICATION_NAME` only if the app's name differs from the service name (it does not; keep service names equal to `spring.application.name`).
- `api-gateway-app` publishes `${BYTECHEF_PORT:-8080}:8080` and `depends_on` configuration-app and execution-app healthy; no other app publishes ports.
- `migrate`: `image: ${BYTECHEF_IMAGE:-docker.bytechef.io/bytechef/bytechef:latest}`, `command: ["liquibase"]`, the environment anchor with `SPRING_LIQUIBASE_ENABLED: "true"` and `BYTECHEF_UPGRADE_ENABLED: "true"`, `restart: "no"`, depends on postgres healthy.
- `mailpit`: `axllent/mailpit`, publishes `${MAILPIT_UI_PORT:-8025}:8025`.
- Volumes `postgres`, `file-storage`. A header comment lists the required variables and the build commands from Task 2 Step 1.

- [ ] **Step 1: Verify the missing-secret failure**

Run: `docker compose -f docker-compose.microservices.yml config >/dev/null` with no variables set.
Expected: FAIL naming `POSTGRES_PASSWORD` (before the file exists: "no such file").

- [ ] **Step 2: Write the file** as above.

- [ ] **Step 3: Validate**

Run: `docker compose -f docker-compose.microservices.yml config >/dev/null` without variables (expect the `POSTGRES_PASSWORD` error), then with `POSTGRES_PASSWORD=x RABBITMQ_PASSWORD=x BYTECHEF_ENCRYPTION_KEY=$(openssl rand -base64 16) BYTECHEF_SECURITY_REMEMBER_ME_KEY=x BYTECHEF_REMOTE_GRPC_SERVICE_TOKEN=x` (expect success), and `docker compose -f docker-compose.microservices.yml config | grep -c 'published'` (expect 2: gateway and Mailpit UI).

- [ ] **Step 4: Bring it up**

Run: with the variables above and `BYTECHEF_<APP>_IMAGE` pointing at the local tags, `docker compose -f docker-compose.microservices.yml up -d --build --wait`
Expected: every service healthy and `migrate` exited 0. Fix configuration until it is; each fix that changes code gets a test in the owning module first.

- [ ] **Step 5: Commit**

```bash
git add docker-compose.microservices.yml
git commit -m "Add the Docker Compose file for the EE microservices"
```

---

### Task 4: Live milestone 1 run

**Files:** whatever the run shows is missing. Stub calls the flow hits are implemented over gRPC following plan 3's pattern (proto in the owning `*-remote-grpc-proto`, server in `*-remote-grpc` gated `@ConditionalOnClass` on the implementation, client method in `*-remote-client` with `get*` = fetch + the local exception), each with a wire test written and watched to fail first. A REST client that already works stays REST (plan 3b ports it).

Checklist, all through `http://localhost:8080` in a browser driven by Playwright (or Chrome DevTools) MCP, with the Compose stack from Task 3:
1. Sign up a new user (no activation mail needed) and log in; the home page loads its GraphQL data (configuration-app reads the gateway's session).
2. Create a project and a workflow; add a component task.
3. Create a connection for a component that needs one (any API-key component; a fake key is fine as long as the connection saves).
4. Run the workflow from the editor; the result shows in the editor's run panel.
5. Add a webhook trigger; the editor shows a URL on `http://localhost:8080/webhooks/...`; `curl` it; a new execution appears.
6. Add a schedule trigger (every minute), enable the workflow, wait two minutes: exactly two executions.
7. Execution history lists every run above with status and output.
8. `docker compose restart configuration-app`; after it is healthy the same browser session still works without logging in.
9. A sign-up mail (invite a user from the admin page) is visible in Mailpit at `http://localhost:8025`.

- [ ] **Step 1: Run the checklist** and record each failing item in the ledger with the error (browser console, network response, the failing service's log).
- [ ] **Step 2: For each failure,** write the test that reproduces it in the owning module, watch it fail, fix, run that module's `check`, rebuild the affected image, and rerun the checklist from the failed item.
- [ ] **Step 3: Run the full checklist once more from a clean stack** (`docker compose -f docker-compose.microservices.yml down -v` then `up -d --build --wait`).
Expected: all nine items pass.
- [ ] **Step 4: Run the touched modules' checks and the monolith**

Run: `./gradlew <touched modules>:check :server:apps:server-app:testIntegration`
Expected: PASS.

- [ ] **Step 5: Commit** each fix as its own commit as it lands (`git add <module paths>`; subject names the behavior fixed).

---

### Task 5: Documentation

**Files:**
- Modify: `docs/content/docs/platform/use-bytechef/self-hosted/installation/distributed.mdx`
- Modify: `CLAUDE.md` (EE microservices list: the gateway owns users and login; gRPC between services)

The page today describes `config-server-app` as every app's configuration source, lists `ai-copilot-app`, and gives a bring-up order through the config server. Rewrite it for this layout, keeping the sections that still hold (worker queues and scaling, scheduler backends, plan-limit enforcement, crash recovery) and correcting their config-server references:
- The services table (eight apps; the gateway owns users, login, the user GraphQL and serves the client; `runtime-job-app` and `ai-gateway-app` are not in the Compose file).
- How a request flows: browser to gateway, session in Redis read by every browser-facing app, gRPC between services with the service token, discovery through Redis.
- Configuration: environment variables only, the required ones, the trusted-network switch and what TLS needs instead.
- Running it: the build commands, `docker compose -f docker-compose.microservices.yml up -d --build --wait`, Mailpit, and the migrate service.
- Current limitations: single tenant verified; embedded API-key auth on execution-app and webhook-app not yet available; one database for all services.

- [ ] **Step 1: Check the page builds**

Run: `cd docs && npm run build 2>&1 | tail -20` (or the docs site's lint/typecheck if a full build is unavailable)
Expected: PASS before and after the edit.

- [ ] **Step 2: Rewrite the page and the CLAUDE.md lines.** Plain language, no em dashes (`grep -n '—' distributed.mdx` prints nothing).

- [ ] **Step 3: Rebuild the docs** with the Step 1 command. Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add docs/content/docs/platform/use-bytechef/self-hosted/installation/distributed.mdx CLAUDE.md
git commit -m "docs - Describe the EE microservices deployment with the gateway, gRPC and Docker Compose"
```
