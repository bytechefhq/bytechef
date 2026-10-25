# EE Microservices Deployment (Docker Compose)

## Intent

ByteChef gets a second distributed deployment next to the multi-node monolith cluster (#6616,
`docker-compose.cluster.yml`). The monolith cluster runs one image in several roles; this one runs the existing EE
microservice apps in `server/ee/apps/*` as separate services, organized the way JHipster organizes its microservices:
a gateway that is the front door and owns users, and backend services behind it.

The apps exist but have never run together. Many of their inter-service calls are unimplemented stubs, there are no
images, and the config server and gateway routes are leftovers from an earlier sketch. The goal is to make the set
real: start it with one `docker compose up` and run a workflow end to end.

## Goals

- One Docker Compose file that starts the microservice deployment, built like `docker-compose.cluster.yml`: a shared
  environment anchor, required secrets fail fast with `${VAR:?...}`, topology chosen with variables and defaults
  (never by editing the file), health checks everywhere, a one-shot `migrate` service that runs Liquibase before any
  app starts.
- Services: `api-gateway-app`, `configuration-app`, `execution-app`, `webhook-app`, `coordinator-app`, `worker-app`,
  plus `connection-app` and `scheduler-app` (needed for connections and scheduled/polling triggers).
- Infrastructure containers: PostgreSQL, Redis, and the message broker.
- One Docker image per app.
- Service-to-service calls use gRPC. REST between services goes away.
- Milestone 1 acceptance: through the gateway on the published port, a user signs up or logs in, creates a project
  and workflow in the UI, creates a connection, runs the workflow from the editor, triggers it via a webhook and via a
  schedule, and sees execution history.

## Non-goals

- Kubernetes/Helm for the microservices (later).
- `ai-gateway-app` and `runtime-job-app` in the compose file.
- Implementing every empty stub. Only the stub calls the milestone 1 path needs; the rest stay stubs.
- JWT/token-based browser auth. A gateway-side token relay (BFF) may come later; it layers on top of this design.
- Making the microservices a CE feature. Everything here is EE (`bytechef.edition=ee`).

## Anti-goals

- Secrets served over HTTP (the reason the config server is dropped).
- A rewrite of ByteChef's login paths (form, remember-me, social, SSO, API keys, embedded connected users, MCP OAuth).
- Any change in behavior of the monolith or the #6616 cluster.
- Two ways to call a remote service. gRPC replaces REST; no transport flag is kept.

## Decisions (made by the user)

1. **Layout borrowed from JHipster.** The gateway is the front door: it serves the React client, routes HTTP to the
   services, and owns user management (account, sign-up, activation, password reset, user admin) using
   `platform-user-service` and the user tables. Login moves there too: form login, remember-me, social login and SSO,
   and the user GraphQL (`platform-user-graphql`, `user.graphqls`).
2. **Login stays session based, using Spring Session.** The browser logs in at the gateway; the session is stored in
   Redis through the EE `session-config` module from #6616 (`bytechef.session.store=redis`). Every service that
   serves browser requests reads the same session, so no sticky routing and no JWT.
3. **No config server.** The per-app YAML in `config-server-app/src/main/resources/config/apps/` moves into each app.
   The shared `apps/application.yml` (management, metrics, `info.*`, OpenAPI base paths) goes into a small common
   config module every app depends on, not copied into each app. Deployment values come from Compose environment
   variables with defaults (Spring Boot relaxed binding, as in `docker-compose.cluster.yml`). The
   `optional:configserver:http://localhost:6111` import is removed from every app.
4. **Internal calls use gRPC instead of REST.** The template is the POC commit `f3da212d18a` on `0_732` ("Add gRPC
   remote-transport POC for ConnectionService"): a `*-remote-grpc-proto` module with the `.proto` contract (typed
   fields, `google.protobuf.Struct` for opaque maps), a `*-remote-grpc` module with mapper, server and client, a
   discovery `NameResolver` over the existing Redis discovery, and interceptors propagating an internal service token
   and the tenant context. The POC is ported to master first and then repeated per service. The `*-remote-rest`
   modules, the REST remote clients and the `bytechef.remote.transport` property are removed.
5. **Docker Compose** is the delivery.
6. **The existing microservice apps are used**, not the monolith image in roles.
7. **One shared database for now.** All apps point at the same PostgreSQL database; their Liquibase contexts run
   against it. A database per service is a later step.
8. **Every REST remote client that works today is ported to gRPC** (32 of the 75), so nothing that works now
   regresses. The 43 empty stubs are implemented only where the milestone 1 path needs them.
9. **`config-server-app` and `ai-copilot-app` are deleted:** the app modules, their `settings.gradle.kts` includes and
   their `CLAUDE.md` lines. Only the app modules go; libraries they used (for example
   `server/ee/libs/ai/ai-copilot/*`) stay, since other apps and the monolith use them.

## Decisions (Claude's calls, open to override)

- **Images:** one image per app, following JHipster. Built from each app's boot jar the way `runtime-job-app/Dockerfile`
  does it (GraalVM base), from a shared Dockerfile with the app name as a build argument. Compose uses
  `BYTECHEF_<APP>_IMAGE` variables with `docker.bytechef.io/bytechef/<app>:latest` defaults, so a locally built image
  can be swapped in.
- **connection-app and scheduler-app are included.** `configuration-app` has no connection code of its own (it uses
  `platform-connection-remote-client`), and Quartz lives in `scheduler-app`.
- **Sessions only where browsers land:** gateway, configuration, execution, connection, webhook (editor webhook test).
  Coordinator, worker and scheduler talk only gRPC and the broker and get no `session-config`.
- **gRPC transport in Compose:** the POC requires TLS and allows plaintext only to loopback, which fails between
  containers. Add an explicit "trusted network" plaintext switch, set on in the compose file; TLS remains the default
  everywhere else.
- **Only the gateway port is published.** Every other service is reachable only on the Compose network.
- **Message broker:** `amqp` (RabbitMQ) default, `redis` selectable with `BYTECHEF_MESSAGE_BROKER_PROVIDER`, matching
  the cluster file. Redis is always present anyway (discovery, sessions).
- **Two GraphQL owners:** the client sends every operation to `/graphql`, which the gateway cannot split by
  operation. The gateway serves the user GraphQL on its own path, and the client's GraphQL fetcher sends user
  operations there; everything else keeps going to `/graphql` on configuration-app. No schema stitching or federation.
- **Per-request token auth stays put:** API keys, embedded connected-user auth and MCP OAuth tokens authenticate each
  request on the service that receives it, as today. Only interactive login moves to the gateway.
- **Tenancy:** single tenant by default, multi tenant via `BYTECHEF_TENANT_MODE=multi`, matching the cluster file.
  Milestone 1 is verified in single-tenant mode; multi-tenant `migrate` (schema per tenant) is a follow-up.
- **Mail:** compose includes Mailpit (`axllent/mailpit`, as in `server/docker-compose.dev.infra.yml`; SMTP 1025,
  UI 8025) so sign-up and password reset can be exercised; activation stays off by default
  (`bytechef.sign-up.activation-required=false`), and the seeded admin can log in without email.
- **Shared session classes:** every browser-facing app must be able to read a session written by any other one. The
  security context and principal classes must be on all of their classpaths (or the session serialized in a
  classpath-independent way). This is done in this project, not by changing the open #6616 branch.
- **Builds on #6616.** That branch adds `session-config`, the redis session store and the cluster compose patterns;
  it merges first (after #6621).

## Current state (verified on master, 2026-10-25)

- `api-gateway-app`: Spring Cloud Gateway (WebMVC) plus `discovery-redis`. No security, no database, no static
  client.
- Gateway routes (in the config server's `apigateway-app.yml`) overlap: `/api/automation/**` and `/api/embedded/**`
  map to configuration-app, connection-app and execution-app at once, so the first match wins. Routes must become
  precise per controller path.
- `configuration-app` contains `platform-security-service`, `platform-security-graphql`, `platform-oauth2-service`
  and `platform-user-remote-client`. No app hosts `platform-user-service`; that remote client has nothing to call
  today.
- 75 remote client classes under `server/ee/libs/**/*-remote-client`; 43 of them have every method throwing
  `UnsupportedOperationException`. 11 `*-remote-rest` modules exist.
- No images for the apps; only `runtime-job-app` has a Dockerfile. The monolith `Dockerfile` copies `client/dist`
  into the image; the gateway needs the same.
- Each app with a database uses `bytechef.datasource.*` and its own Liquibase context (`configuration`, `execution`,
  `connection`, `scheduler`); the dev configs point them at separate databases, which this project replaces with one.
- 32 of the 75 remote clients are real REST implementations over the 11 `*-remote-rest` modules.
- `session-config` exists only on branch 6616. Memory notes the EE apps also lack `cache-invalidation-config` (#6639).

## Shape of the work

1. **Foundation:** move per-app YAML into the apps, drop the config server import, fix gateway routes, add
   `session-config` to the browser-facing apps.
2. **Gateway owns users:** gateway gets a datasource, `platform-user-service`, the user GraphQL/REST controllers and
   Spring Security with the redis session; serves the static client. configuration-app's user lookups go to the
   gateway over gRPC.
3. **gRPC foundation:** port the POC to master; extract the shared parts (discovery name resolver, interceptors,
   channel/server config, plaintext switch) into a common module so each service only adds proto, mapper, server and
   client.
4. **gRPC per service:** port the 32 working REST clients to gRPC, implement the stub calls the milestone 1 flow
   hits, then delete the `*-remote-rest` modules and REST clients.
5. **Images and compose:** per-app images, the compose file, a `migrate` service.
6. **Remove dead apps and update docs:** delete `config-server-app` and `ai-copilot-app`; rewrite the public page
   `docs/content/docs/platform/use-bytechef/self-hosted/installation/distributed.mdx`, which today describes the
   config server as every app's configuration source and lists `ai-copilot-app`. It should describe this layout:
   gateway with users and login, gRPC between services, environment variables, the compose file.
7. **Live verification:** run the milestone 1 flow against the compose stack.

## Left to the builder

- Exact gateway route table (derive from controller base paths per app).
- Which stub calls the milestone path needs (derive by running the flow, not by guessing).
- How `migrate` runs Liquibase for all contexts (one app image with all changelogs, or each app migrating its own
  context with startup ordering).
- Module names for the shared gRPC code.
- Commit and PR breakdown, following the repo conventions (Liquibase changes in their own commits, generated files in
  their own commits).
