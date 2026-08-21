# AWS S3 Chat Memory & Session — Design

- **Date:** 2026-06-08
- **Branch:** `0_732`
- **Status:** Approved for planning

## Summary

Add AWS S3-backed chat memory and session support to ByteChef, modeled on the (closed-unmerged)
Spring AI PR [#5091](https://github.com/spring-projects/spring-ai/pull/5091). The work spans **six
implementations**: two self-contained vendored Spring AI modules (an S3 `ChatMemoryRepository` and an
S3 `SessionRepository`) plus four ByteChef consumers (two connection-backed components and two
built-in/platform extensions). The built-in path is **multi-tenant via bucket-per-tenant**.

### Context: why both a repository and a session impl

Spring AI is deprecating the `ChatMemoryRepository` abstraction in favor of the `spring-ai-session`
abstraction (Spring AI 2.1); PR #5091 was closed on 2026-05-20 with that guidance. There will never
be an upstream `org.springframework.ai:...-repository-s3` artifact, so ByteChef vendors its own copy
(consistent with how it already vendors the JDBC/Redis/Mongo/Neo4j/Cassandra repositories). Building
both a repository impl and a session impl aligns with the migration direction.

## Decisions

1. **Storage engine:** S3 for **both** modules (one JSON object per conversation / per session).
2. **Built-in shape:** **extend** the existing built-in storage selection (config layer), do not add
   new built-in components.
3. **Multi-tenancy (built-in only):** **bucket per tenant** — bucket `{bucketPrefix}-{tenantId}`,
   auto-created on first use.
4. **Session provider key:** new `bytechef.ai.session.provider` property (independent of
   `bytechef.ai.memory.provider`). Provider value for both is `aws` (matches the `-aws` module
   naming); the Spring AI classes/packages keep `S3…` / `…repository.s3` since they name the
   storage technology.
5. **Credentials:** explicit access-key/secret when provided; otherwise fall back to the default AWS
   credential chain (IAM roles in prod).

## Module layout

New repo-root `spring-ai/` directory (mirrors the existing `spring-ai-agent-utils/` root module).

| # | Deliverable | Path | Key class(es) | Package |
|---|---|---|---|---|
| 1 | repo (S3) | `spring-ai/spring-ai-model-chat-memory-repository-aws` | `S3ChatMemoryRepository` (+ builder) | `org.springframework.ai.chat.memory.repository.s3` |
| 2 | session (S3) | `spring-ai/spring-ai-session-aws` | `S3SessionRepository` (+ builder) | `org.springframework.ai.session.s3` |
| 3 | component | `server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws` | `AwsChatMemoryComponentHandler`, `AwsChatMemory` (cluster), `AwsChatMemoryConnection` | `com.bytechef.component.ai.agent.chat.memory.aws` |
| 4 | component | `server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws-session` | `AwsSessionChatMemoryComponentHandler`, `AwsSessionChatMemory` (cluster) | `com.bytechef.component.ai.agent.chat.memory.aws.session` |
| 5 | built-in chat memory (extend) | `server/libs/config/ai-chat-memory-config/ai-chat-memory-aws-config` (new) | `AwsChatMemoryConfiguration`, `AwsChatMemoryProperties`, `TenantRoutingS3ChatMemoryRepository` | `com.bytechef.ai.chat.memory.aws.config` |
| 6 | built-in session (extend) | edit `server/libs/modules/components/ai/agent/chat-memory/chat-memory-builtin-session` | extend `BuiltInSessionChatMemory` + new `TenantRoutingS3SessionRepository` | existing package |

The two `spring-ai/*` modules are **self-contained** (repository + builder, no separate
autoconfigure module — that is why the count is 6, not 8). They depend only on the relevant Spring AI
artifact + `software.amazon.awssdk:s3` + Jackson, and **never on ByteChef code**, so they stay
cleanly portable. The ByteChef config layer constructs them directly, exactly as
`ai-chat-memory-jdbc-config` wraps `JdbcChatMemoryRepository` today.

## The two Spring AI modules

### #1 `S3ChatMemoryRepository implements ChatMemoryRepository`

Port of PR #5091.

- One JSON object per conversation at `{keyPrefix}/{conversationId}.json` holding the serialized
  `List<Message>`.
- `add` / `get` / `clear` = get-modify-put (`GetObject` → mutate → `PutObject`).
- `findConversationIds()` returns IDs ordered by object `lastModified` (descending), so the built-in
  handler does **not** need the `OrderedJdbcChatMemoryRepository` wrapper — ordering is intrinsic.
- `findByConversationId(id)` deserializes the object (empty list when absent).
- Builder: `S3Client`, `bucketName`, `keyPrefix`, optional `keyResolver` (`Function<String,String>`
  conversationId → S3 key) for callers that derive keys per call.

### #2 `S3SessionRepository implements SessionRepository`

Implements the full `org.springframework.ai.session.SessionRepository` contract (sessions + a
separately-versioned event log with compare-and-swap).

- One JSON object per session at `{keyPrefix}/{sessionId}.json` holding
  `{ session metadata, events[], version }`.
- `version` field backs `getEventVersion(id)`.
- `replaceEvents(id, events, expectedVersion)` → **conditional `PutObject`** using the object ETag
  captured alongside the events at read time (`If-Match`). A `412 PreconditionFailed` maps to a
  `false` return (concurrent writer already mutated). This is the S3-native realization of the CAS
  contract (S3 conditional writes, GA 2024).
- `appendEvent(event)` = read-modify-write, incrementing `version`; updates session `lastActiveAt`.
- `replaceEvents(id, events)` (non-CAS) = unconditional `PutObject`.
- `save` / `findById` / `delete` = put / get / delete of the session object.
- `findByUserId` / `findExpiredSessionIds` = `ListObjects` + scan (documented O(n)).
- `findEvents(id, filter)` honors `EventFilter.lastN()`; returns empty list for unknown session
  (per contract).
- Builder: `S3Client`, `bucketName`, `keyPrefix`, `JsonMapper`, optional `keyResolver`.

## Components #3 & #4 (connection-backed)

Mirror the JDBC pair (`chat-memory-jdbc`, `chat-memory-jdbc-session`), except where the JDBC variants
resolve a child `DATA_SOURCE` cluster element, the AWS variants carry their **own connection**.

### #3 `chat-memory-aws`

- `AwsChatMemoryConnection`: `accessKeyId`, `secretAccessKey`, `region`, `bucket`, optional
  `keyPrefix` (same shape as the existing `AwsS3Connection`).
- `AwsChatMemory` cluster element, `type(CHAT_MEMORY)`: builds an `S3Client` from connection params,
  constructs `S3ChatMemoryRepository`, wraps in `MessageWindowChatMemory` + `MessageChatMemoryAdvisor`,
  returns `ChatMemoryFunction.Result(advisor, chatMemory)`.
- `CONVERSATION_ID` dropdown `OptionsFunction` lists conversations via `findConversationIds()` (mirror
  of `JdbcChatMemoryUtils.getClusterElementFirstMessages`), reading the connection directly.

### #4 `chat-memory-aws-session`

- `AwsSessionChatMemory` cluster element, `type(SESSION_REPOSITORY)`: builds `S3Client` from its
  connection, returns `S3SessionRepository`. No `DATA_SOURCE` child.

For #3 and #4 there is **no tenant logic** — the user-supplied bucket is the isolation boundary.

## Built-in #5 — chat memory (config-layer extension)

New module `ai-chat-memory-aws-config`, parallel to `ai-chat-memory-jdbc-config`:

```java
@Bean
@ConditionalOnProperty(prefix = "bytechef.ai.memory", name = "provider", havingValue = "aws")
ChatMemory awsChatMemory(S3Client s3Client, AwsChatMemoryProperties props) {
    ChatMemoryRepository repository =
        new TenantRoutingS3ChatMemoryRepository(s3Client, props.bucketPrefix(), props.keyPrefix());

    return MessageWindowChatMemory.builder()
        .chatMemoryRepository(repository)
        .maxMessages(500)
        .build();
}
```

`TenantRoutingS3ChatMemoryRepository implements ChatMemoryRepository` lives in this config module
(which may depend on `tenant-api`). It holds a Caffeine cache `tenantId → S3ChatMemoryRepository`,
each bound to bucket `{bucketPrefix}-{tenantId}` and ensuring the bucket exists on first use. Every
method reads `TenantContext.getCurrentTenantId()`, resolves the per-tenant repository, delegates —
echoing `InMemorySessionRepositoryHolder`'s per-tenant Caffeine pattern, keyed onto buckets.

`ChatMemoryComponentHandler` is **unchanged**: it injects whatever `ChatMemory` / `ChatMemoryRepository`
bean exists. Its existing `OrderedJdbcChatMemoryRepository` wrapping only fires when a `JdbcTemplate`
is present, so the S3 path is untouched (ordering comes from the S3 repository). Wiring registered via
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## Built-in #6 — session (extend the existing handler)

The session built-in has no config layer today: `BuiltInSessionChatMemory.of(JdbcTemplate)` hardcodes
"JdbcTemplate present → JDBC repo, else in-memory". Extend it to consult `bytechef.ai.session.provider`
(default preserves today's behavior; `aws` selects S3). When `aws`, build a
`TenantRoutingS3SessionRepository` (same Caffeine-per-tenant-bucket pattern). The S3 wiring and the
routing wrapper live in the `chat-memory-builtin-session` module, which gains deps on
`spring-ai-session-aws` + `tenant-api` + `awssdk:s3`. This is the **only** place that edits an existing
file/test — an accepted asymmetry driven by the session built-in's inline wiring.

## Configuration & registration

- **Properties:**
  - `bytechef.ai.memory.provider=aws` + `bytechef.ai.memory.aws.{bucket-prefix,region,access-key-id,secret-access-key,key-prefix}`
  - `bytechef.ai.session.provider=aws` + `bytechef.ai.session.aws.{bucket-prefix,region,access-key-id,secret-access-key,key-prefix}`
  - Blank access-key/secret → default AWS credential chain.
- **`settings.gradle.kts`:** add `spring-ai/spring-ai-model-chat-memory-repository-aws`,
  `spring-ai/spring-ai-session-aws`, `chat-memory-aws`, `chat-memory-aws-session`,
  `ai-chat-memory-aws-config`.
- **Dependency:** `software.amazon.awssdk:s3` (no explicit version — managed by the platform BOM, as
  `aws-s3` does).
- **Licensing:** all paths are CE (`server/libs/**`, `spring-ai/**`) → Apache header, no `@version ee`.

## Testing

- **Spring AI modules:** unit/integration tests against **LocalStack via Testcontainers** (`*IntTest`),
  explicitly covering the session CAS path (concurrent `replaceEvents` → exactly one `412`/`false`).
- **Components:** handler tests auto-generate JSON definition files in `src/test/resources/definition/`
  — delete stale `.json` and `build/resources/test/definition/` before regenerating.
- **Config module:** verify provider-conditional bean wiring and per-tenant bucket routing
  (mock `S3Client`, assert bucket name derivation from `TenantContext`).

## Known limitations (consequences of "S3 for both")

- Chat-memory `add` is read-modify-write, not atomic — acceptable for single-writer-per-conversation
  (the normal agent loop), but lost updates are possible under concurrent writes to one conversation.
- `findByUserId` / `findExpiredSessionIds` are O(n) `ListObjects` scans.
- Bucket-per-tenant is bounded by the AWS ~100–1000 buckets/account soft limit (raisable on request);
  noted for operators.

## Out of scope

- DynamoDB backends (explicitly deferred; S3 chosen for both).
- A separate `spring-ai-autoconfigure-*` module per backend (config constructed in the ByteChef layer).
- Migrating existing JDBC/Redis chat memory to the session abstraction.
