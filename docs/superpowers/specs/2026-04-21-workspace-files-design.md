<!-- Source: https://docs.sim.ai/mothership/files#creating-documents -->

# Workspace Files — V1 Design

**Date:** 2026-04-21
**Status:** Draft
**Summary:** A workspace-scoped *Files* capability in ByteChef, parallel to Knowledge Bases and Data Tables, with conversational AI generation via the existing Copilot. Mirrors the Sim.ai Mothership "Creating Documents" feature in the ByteChef context.

---

## 1. Scope & Feature Summary

### User-visible capabilities (V1)
1. Upload any file (drag-and-drop or REST) into the Files panel — stored in the workspace, browsable, downloadable.
2. Ask Copilot from anywhere ("write a markdown runbook for this failure", "draft a CSV of test accounts") — file appears in the Files panel.
3. Open a Files-scoped Copilot agent from the Files panel itself for document-authoring sessions.
4. View/edit text-like files (markdown, CSV, JSON, code, txt) inline in a Monaco-based editor. Binary files: image/PDF preview where possible; otherwise download.
5. Rename, delete (hard), and download.

### Out of scope for V1
- Version history (shape is forward-compatible — see §2.5)
- Folder hierarchy (flat list within a workspace)
- Cross-workspace sharing, project association
- RAG/chunking/embedding (remains `KnowledgeBase`'s responsibility)
- AI-generated binary outputs (images, spreadsheets)
- Preview thumbnails in the list (only in the detail sheet)

### Module naming
`automation-workspace-file` — sub-modules `-api`, `-file-storage`, `-service`, `-rest`, `-graphql`. Explicit to avoid collision with `core/file-storage` and `knowledge-base-document`.

### EE / CE split
- **CE** — `server/libs/automation/automation-workspace-file/*`: upload, CRUD, viewer, editor.
- **EE** — `server/ee/libs/automation/automation-workspace-file/automation-workspace-file-ai/automation-workspace-file-ai-service`: `CreateWorkspaceFileToolCallback` and its companion read-tools. `FilesSpringAIAgent` itself lives in `ai-copilot-app` alongside the other agents.

---

## 2. Data Model

### 2.1 `WorkspaceFile` (`@Table("workspace_file")`)
```
Long        id                       // @Id
String      name                     // filename incl. extension
String      description              // nullable
String      mimeType
long        sizeBytes
FileEntry   file                     // stored blob reference (base64 in column)
short       source                   // WorkspaceFileSource ordinal
Short       generatedByAgentSource   // nullable; Copilot Source ordinal if AI-generated (matches `source` column width)
String      generatedFromPrompt      // nullable; audit of originating prompt
Set<WorkspaceFileTag> workspaceFileTags   // @MappedCollection

Instant     createdDate
String      createdBy
Instant     lastModifiedDate
String      lastModifiedBy
int         version                  // optimistic locking
```

### 2.2 `WorkspaceFileTag`
Mirrors `KnowledgeBaseDocumentTag` / `DataTableTag` — `(workspace_file_id, tag_id)` join rows.

### 2.3 `WorkspaceWorkspaceFile` (`@Table("workspace_workspace_file")`)
Join entity per the existing pattern in `WorkspaceKnowledgeBase` / `WorkspaceDataTable`. Columns:
`id, workspace_id, workspace_file_id, created_by, created_date, last_modified_by, last_modified_date, version`. Unique index on `(workspace_id, workspace_file_id)`.

### 2.4 Enum `WorkspaceFileSource`
`USER_UPLOAD(0)`, `AI_GENERATED(1)` — persisted as INT ordinal per project convention. Append new values only at the end.

### 2.5 Forward compatibility toward versioning
When history is added later: `WorkspaceFile.file / sizeBytes / mimeType` move into a `workspace_file_version` child table; `WorkspaceFile` grows `currentVersionId`. Existing rows 1-to-1 backfill into single versions.

### 2.6 Liquibase changelog `workspace_file.xml`
- `workspace_file` table (all columns above).
- `workspace_file_tag` join.
- `workspace_workspace_file` join, unique index `(workspace_id, workspace_file_id)`.
- Secondary indexes: `workspace_workspace_file(workspace_id)`, `workspace_file(name)`.

---

## 3. Service Layer & Module Wiring

### 3.1 Service interfaces (`automation-workspace-file-api`)
```java
WorkspaceFileService
  create(WorkspaceFile file, Long workspaceId): WorkspaceFile
  fetchByName(Long workspaceId, String name): Optional<WorkspaceFile>
  findById(Long id): WorkspaceFile
  findAllByWorkspaceId(Long workspaceId, List<Long> tagIds): List<WorkspaceFile>
  update(WorkspaceFile file): WorkspaceFile
  delete(Long id): void

WorkspaceFileFacade
  createFromUpload(Long workspaceId, String filename, String contentType, InputStream): WorkspaceFile
  createFromAi(Long workspaceId, String filename, String contentType, String content,
               Source generatedByAgentSource, String generatedFromPrompt): WorkspaceFile
  updateContent(Long id, String contentType, InputStream data): WorkspaceFile
  downloadContent(Long id): InputStream
  rename(Long id, String newName): WorkspaceFile
  delete(Long id): void   // deletes both blob and row

WorkspaceFileTagService
```

### 3.2 File storage wrapper (`automation-workspace-file-file-storage`)
Thin facade over `FileStorageService`. Directory fixed to `"workspace-files"`. Provider choice (JDBC/filesystem/S3) inherited from workspace config.

### 3.3 Module dependencies
- `-service` → `-api`, `core/file-storage`, `platform-tag`, `-file-storage`
- `-rest` → `-service`, `-api`
- `-graphql` → `-service`, `-api`
- `-ai-service` (EE) → `-api`, Spring AI

### 3.4 Spring registration
`AutomationWorkspaceFileJdbcRepositoryConfiguration` with `@EnableJdbcRepositories` + `@ConditionalOnBean(AbstractJdbcConfiguration.class)`, registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (per `CLAUDE.md` convention for new Spring Data JDBC modules).

### 3.5 Search & metrics
- `WorkspaceFileSearchAssetProvider` — mirrors `DataTableSearchAssetProvider`.
- `bytechef_workspace_file_create` Counter (Micrometer) — tags `source`, `mime_type_category`. Wired via `ObjectProvider<MeterRegistry>` so lightweight app variants start cleanly.

### 3.6 Hardening
- **Filename sanitization** — strip path separators and control chars, NFC-normalize, clamp to 255 chars; on collision append `-2`, `-3`, etc.
- **Content-type sniffing** — Apache Tika (transitive from KB); persisted mime type = Tika result, not `Content-Type` header.
- **Quota** — `bytechef.workspace-file.max-file-size-bytes` (default 25 MB) and `bytechef.workspace-file.per-workspace-total-bytes` (default 1 GB, `-1` = unlimited). Enforced in `WorkspaceFileFacade` before streaming to storage.
- **Text edit threshold** — `bytechef.workspace-file.max-text-edit-bytes` (default 1 MB); `workspaceFileTextContent` rejects larger files with a "download instead" error.

---

## 4. REST + GraphQL Surface

### 4.1 REST (`automation-workspace-file-rest`)
Multipart + binary-stream paths that are awkward over GraphQL.

```
POST   /api/automation/internal/workspace-files/upload
       multipart: workspaceId, file
       → 201 { id, name, mimeType, sizeBytes, createdDate, ... }

GET    /api/automation/internal/workspace-files/{id}/content
       → streams raw file with Content-Disposition: attachment

PUT    /api/automation/internal/workspace-files/{id}/content
       multipart: file (overwrites content in place)
       → 200 { ...updated entity... }
```

### 4.2 GraphQL schema
```graphql
type WorkspaceFile {
  id: ID!
  name: String!
  description: String
  mimeType: String!
  sizeBytes: Int!
  source: WorkspaceFileSource!
  generatedByAgentSource: String          # Source enum name, nullable
  generatedFromPrompt: String             # nullable
  tags: [Tag!]!
  createdBy: String
  createdDate: DateTime
  lastModifiedBy: String
  lastModifiedDate: DateTime
  downloadUrl: String!
}

enum WorkspaceFileSource {
  USER_UPLOAD
  AI_GENERATED
}

extend type Query {
  workspaceFile(id: ID!): WorkspaceFile
  workspaceFiles(workspaceId: ID!, tagIds: [ID!], mimeTypePrefix: String): [WorkspaceFile!]!
  workspaceFileTags(workspaceId: ID!): [Tag!]!
  workspaceFileTextContent(id: ID!): String
}

extend type Mutation {
  updateWorkspaceFile(input: UpdateWorkspaceFileInput!): WorkspaceFile!
  updateWorkspaceFileTextContent(id: ID!, content: String!): WorkspaceFile!
  deleteWorkspaceFile(id: ID!): Boolean!
  updateWorkspaceFileTags(id: ID!, tagIds: [ID!]!): WorkspaceFile!
}

input UpdateWorkspaceFileInput {
  id: ID!
  name: String
  description: String
}
```
Enum values `SCREAMING_SNAKE_CASE` per project GraphQL convention.

### 4.3 Security
Every query/mutation that takes an `id` resolves the file's workspace via the join table and checks caller membership — same pattern used by `DataTable` and `KnowledgeBase`.

---

## 5. Copilot Integration

### 5.1 `CreateWorkspaceFileToolCallback`
Location: `server/ee/libs/automation/automation-workspace-file/automation-workspace-file-ai/automation-workspace-file-ai-service/`.

```java
public record CreateWorkspaceFileInput(
    String filename,       // required, incl. extension (e.g. "auth-spec.md")
    String mimeType,       // required, must match extension
    String content,        // required, full text content
    String description     // optional
) {}

public record CreateWorkspaceFileOutput(
    long id,
    String name,
    String downloadUrl,
    long sizeBytes
) {}
```

Tool description exposed to the model:
> *"Create a new text file in the user's workspace. Use this when the user asks you to write, draft, or generate a document, spec, CSV, JSON, markdown note, or code file. The file will appear in their Files panel. Choose a filename with an appropriate extension."*

Supported mime-type allow-list: `text/markdown`, `text/csv`, `text/plain`, `application/json`, `text/javascript`, `text/x-python`, `text/x-java`, `text/html`, `text/css`, `text/yaml`.

Implementation notes:
- Injected with `WorkspaceFileFacade` + `WorkspaceContextProvider` (resolves current `workspaceId` from agent call context — same mechanism `WorkflowEditorSpringAIAgent` uses to resolve the current workflow).
- Registered as `@Component("createWorkspaceFileToolCallback") @ConditionalOnEEVersion`.
- Out-of-allow-list mime / oversized content return tool-error results so the model can correct and retry.

### 5.2 New `Source.FILES` + `FilesSpringAIAgent`
Both live in `ai-copilot-app`.

- `Source.FILES` added to the existing enum.
- `FilesSpringAIAgent extends SpringAIAgent` with a document-authoring system prompt.
- Tools: `CreateWorkspaceFileToolCallback` (primary) + `ListWorkspaceFilesToolCallback` + `GetWorkspaceFileContentToolCallback`. The latter two are registered only on the Files agent.
- State injected into `AgUiParameters.state`: current `workspaceId`, recent files list (max 50, ordered by lastModified desc; `[id, name, mimeType, sizeBytes, createdDate]`).
- `CopilotApiController` dispatcher gets one new case: `FILES -> filesSpringAIAgent`.

No `deleteWorkspaceFile` tool in V1 (avoid AI-initiated data loss).

### 5.3 Global tool registration
`CreateWorkspaceFileToolCallback` auto-wires into every agent's `ToolCallback[]` via an `ObjectProvider<ToolCallback>` pattern. Existing per-agent tool lists become collection-based so the new tool joins without per-agent modification.

### 5.4 Client ingestion path
Tool-result interception in `CopilotRuntimeProvider`'s `AgentSubscriber`:

```ts
onToolResult: (event) => {
  if (event.toolName === 'createWorkspaceFile' && event.result) {
    const {id, name, mimeType, sizeBytes} = event.result;
    apolloClient.cache.modify({
      fields: {workspaceFiles: (existing = []) => [newFileRef, ...existing]}
    });
    toast.success(`Created "${name}"`);
  }
}
```

No second SSE channel — the Copilot SSE already carries the tool result.

---

## 6. Frontend

### 6.1 Location & routes
- `client/src/pages/automation/workspace-files/` (parallel to `data-tables/`, `knowledge-bases/`).
- Route: `/automation/workspace-files`.
- File view/edit is a **side sheet** overlaid on the list (not a separate route) — matches sim.ai and keeps list context visible.

### 6.2 Sidebar
New "Files" entry between "Knowledge Bases" and "Data Tables", `FileTextIcon`.

### 6.3 List page (`WorkspaceFilesPage`)
- Header: title, tag filter, search input, Upload button, "Create with AI" button.
- Table columns: Name (with mime-type icon), Source badge (`AI_GENERATED` / `USER_UPLOAD`), Size, Tags, Last Modified, row actions menu (Rename, Delete, Download, Edit, View Provenance).
- Whole-table drag-and-drop upload zone.
- Parallel uploads via `useWorkspaceFileUpload`; per-file progress and retry on error.
- Tool-created files prepended with a 200ms background highlight.
- "Create with AI" button opens the Copilot panel with `Source.FILES` via `openCopilotForFiles()` (pattern from `useCopilotPanelStore`).

### 6.4 Side sheet (`WorkspaceFileDetailSheet`)
- Text-like mime & size ≤ max-text-edit-bytes: Monaco editor with language from extension; Save → `updateWorkspaceFileTextContent`. Dirty-state warning on close.
- `image/*`: inline `<img>`.
- `application/pdf`: `<iframe>` embed of download URL.
- Else: "Preview not available" + Download button.
- Right rail: metadata, tags editor, provenance card (prompt truncated with "show more") for AI-generated, "Continue in Copilot" button.

### 6.5 GraphQL operations
In `client/src/graphql/workspace-files/`:
`getWorkspaceFiles.graphql`, `getWorkspaceFile.graphql`, `getWorkspaceFileTextContent.graphql`, `getWorkspaceFileTags.graphql`, `updateWorkspaceFile.graphql`, `updateWorkspaceFileTextContent.graphql`, `updateWorkspaceFileTags.graphql`, `deleteWorkspaceFile.graphql`.
Regenerate `graphql.ts` via `npx graphql-codegen`. Uploads go through `fetch` directly (multipart), not Apollo.

### 6.6 Stores & hooks
- `useWorkspaceFilesStore` (Zustand) — filter, search, selection. Exported for direct state manipulation in tests.
- `useWorkspaceFileUpload` — multipart upload with progress/error via `useFetchInterceptor`.

### 6.7 Client conventions followed (CLAUDE.md)
`twMerge` (not `cn`), interfaces suffixed `I`/`Props`, Lucide icons with `Icon` suffix, hook ordering state→ref→stores→hooks→memos→effects→return, alphabetized imports, object keys sorted ascending.

---

## 7. Error Handling

### 7.1 Server
- **Quota exceeded** → `WorkspaceFileQuotaExceededException` — REST `413`, GraphQL `errorType=QUOTA_EXCEEDED`.
- **Invalid mime (AI tool)** → tool-error result, model can retry.
- **Filename collision** → auto-suffix, never throws.
- **Storage failure mid-upload** → write blob first; row insert in txn, blob deleted on rollback. Mirrors `KnowledgeBaseDocumentFacade`.
- **Rename race** → `@Version` optimistic lock; GraphQL `OPTIMISTIC_LOCK_FAILURE`.
- **Orphan blobs** → `WorkspaceFileOrphanBlobCleaner` (`@Scheduled(fixedDelay = 1h)`), gated by `bytechef.workspace-file.orphan-cleanup.enabled` (default true in monolith, false in worker variants).

### 7.2 Client
- Global toast path via `useFetchInterceptor`; no per-mutation `onError` unless needed for custom behavior.
- Monaco dirty-state navigation guard.
- Upload: per-file progress bar, retry button on failure.

---

## 8. Testing

### 8.1 Server unit (`*Test`, drop `Impl`)
`WorkspaceFileServiceTest`, `WorkspaceFileFacadeTest`, `CreateWorkspaceFileToolCallbackTest`. `@ExtendWith(ObjectMapperSetupExtension.class)` where `JsonUtils` / `MapUtils` are touched.

### 8.2 Server integration (`*IntTest`, Testcontainers PostgreSQL)
- `WorkspaceFileFacadeIntTest` — upload → list → download → rename → delete-cleans-blob; quota enforcement.
- `WorkspaceFileGraphQlControllerIntTest` — every op with membership permission matrix.
- `WorkspaceFileRestControllerIntTest` — multipart upload, streaming download, PUT content.
- `AutomationWorkspaceFileJdbcRepositoryConfigurationIntTest` — smoke test of `@EnableJdbcRepositories` wiring.
- Migration-applies-cleanly standard integration test.
- Metrics counter increments with correct tags.

### 8.3 Client (Vitest)
`WorkspaceFilesPage.test.tsx`, `WorkspaceFileDetailSheet.test.tsx`, `CopilotRuntimeProvider.test.tsx` (new tool-result handler), `useWorkspaceFileUpload.test.ts`. Zustand state reset in `beforeEach`. PostHog global mock per `.vitest/setup.ts`.

### 8.4 E2E (Playwright — one happy path)
Log in → open Files panel → drag markdown → appears → edit → save → refresh → persisted.

### 8.5 Observability
INFO log on every create/update/delete (structured with `workspaceId`, `fileId`). `logger.atDebug()` for tool-callback internals.

---

## 9. Acceptance Criteria

- [ ] CE module tree (`automation-workspace-file/*`) compiles; unit and integration tests pass.
- [ ] EE module tree (`automation-workspace-file/automation-workspace-file-ai/*`) compiles with `@ConditionalOnEEVersion`.
- [ ] Liquibase migration applies cleanly on an empty DB.
- [ ] Upload/download/rename/delete round-trip works over REST and GraphQL with workspace membership enforced.
- [ ] `CreateWorkspaceFileToolCallback` is invokable from workflow-editor copilot and from the Files agent; result prepends to the Files panel list without a page refresh.
- [ ] Monaco editor opens text-like files ≤ 1 MB; images/PDFs preview; other binary falls back to download.
- [ ] `./gradlew check` and `cd client && npm run check` green.

---

## 10. Open Questions

None remaining — all deferred items are explicitly out-of-scope in §1 and §5.2.
