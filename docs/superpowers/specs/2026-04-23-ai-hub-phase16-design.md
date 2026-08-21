# AI Hub Phase 16 — Multi-modal file upload in composer

**Status**: Draft
**Date**: 2026-04-23

## Goal

Close the biggest remaining chat-UX gap: let the user attach local files to a message without leaving the AI Hub. Drag a PDF from Finder, paste a screenshot from clipboard, or click an attach button — the file uploads as a workspace asset file and is automatically `@`-referenced in the next message. Matches the baseline expectation from ChatGPT, Claude.ai, Slack, and Mothership.

## Success in v16

- Dragging a file from the OS into the composer uploads it + inserts an `@`-chip.
- Pasting a clipboard image into the composer uploads it + inserts an `@`-chip.
- A paperclip button in the composer opens the OS file picker; selected files upload + insert `@`-chips.
- Upload progress is visible (spinner or bar per attachment while in flight).
- Failed uploads surface a retry affordance.

## Non-goals (v16)

- **Persistent attachment library** — files land in the existing asset-files workspace; nothing new.
- **Auto OCR / vision-model parsing** — the LLM sees the file via the existing `getAssetFileContent` tool on the next turn; Phase 16 does not transcribe images or PDFs.
- **Rich previews in the composer** — chip shows filename + icon, not a thumbnail.
- **Direct binary stream into the message** without persisting — every upload becomes a real asset file. Cheaper UX + no new transport.
- **Server-side mime-type conversion** — we accept what the browser serves; the backend's allow-list gates what actually persists.

## Supported mime types

Existing allow-list:
- Text: `text/markdown`, `text/plain`, `text/csv`, `text/html`, `text/javascript`, `text/x-python`, `text/x-java`, `text/yaml`, `text/css`, `application/json` (from Phase 1 `CreateAssetFileToolCallback.ALLOWED_MIME_TYPES`).
- Binary: `image/png`, `image/jpeg`, `image/webp`, and `application/vnd.openxmlformats-officedocument.presentationml.presentation` (from Phase 5B `CreateBinaryAssetFileToolCallback`).

Phase 16 extends the binary allow-list with **`application/pdf` and `image/gif`** (pdfs are the most-requested attachment type; GIFs round out image variety).

Unsupported types (e.g. `.zip`, `.exe`, `video/*`) error with "File type not supported" — toast at the composer.

## Architecture

### Client

No server changes beyond the allow-list extensions above. All new behavior is client-side:

1. **Composer drop zone** — wrap the existing Thread composer input in a div with `onDragOver` / `onDragLeave` / `onDrop` handlers. Active drag shows a highlighted overlay ("Drop files to attach").

2. **Paste handler** — attach a `paste` listener to the composer textarea. If the clipboard contains files (`e.clipboardData.files.length > 0`), upload each.

3. **Attach button** — paperclip icon in the composer header row. Opens a hidden `<input type="file" multiple accept="..." />`.

4. **Upload orchestration** — new `useAiHubAttachmentUpload` hook:
   - Accepts a list of `File` objects.
   - For each: reads the file (text via `FileReader.readAsText`, binary via `readAsArrayBuffer` → base64).
   - Routes by mime type: text types → `createAssetFile` REST call; binary types → `createBinaryAssetFile` REST call (or the existing asset-file REST upload path that accepts multipart — check what exists; multipart is simpler for large files).
   - Returns a `Promise<{fileId, name}>` per file.

5. **Composer store integration** — on each successful upload, call `aiHubComposerStore.getState().addReference({kind: 'file', id: String(fileId), name})`. The existing `@`-mention infrastructure then includes these refs in the next turn's AG-UI state.

6. **In-flight UI** — show a small chip with a spinner for each uploading file; replace with the normal `@`-ref chip on success. On failure, show a red chip with a Retry button.

### Files

- `client/src/pages/automation/ai-hub/composer/hooks/useAiHubAttachmentUpload.ts` (new).
- `client/src/pages/automation/ai-hub/composer/AiHubComposerDropZone.tsx` (new wrapper).
- `client/src/pages/automation/ai-hub/composer/AiHubComposer.tsx` — add attach button, paste handler, render drop overlay, wire the hook.
- Tests for each.

### Server (minor)

- `CreateBinaryAssetFileToolCallback` — extend `ALLOWED_MIME_TYPES` with `application/pdf`, `image/gif`. One commit.

Or: check if there's a general asset-file REST upload endpoint that the existing Files page uses (multipart form-data). If yes, use it directly from the client — simpler than going through the `createAssetFile` tool path, and it handles large binaries without base64 inflation. The existing Phase 5B KB upload uses `useUploadKnowledgeBaseDocumentDialog`-style multipart; the asset-files page likely has the same.

Pick the multipart REST path if available.

## Error paths

- **File too large** — the asset-file service has a size limit. Surface the server's error message verbatim as a toast.
- **Disallowed mime type** — check client-side before uploading; toast with supported types.
- **Network error** — retry chip, same behavior as a mutation error in Phase 10E.
- **Quota exceeded** — surface the `AssetFileQuotaExceededException` message.

## Interaction with Phase 6.1 artifacts

Every successful upload is a **file creation** in the workspace. It already goes through `createAssetFile` / `createBinaryAssetFile` paths, which Phase 6.1 instrumented to record `FILE_CREATED` / `BINARY_FILE_CREATED` artifacts in the current conversation. **No new instrumentation needed** — the artifacts flow is automatic.

This also means Phase 12 undo works for uploads: the user can revert an upload within the 30-minute TTL via the conversation sidebar or audit page. Free win.

## Testing

### Server
- Extend `CreateBinaryAssetFileToolCallbackTest` with the two new mime types.

### Client
- `useAiHubAttachmentUpload.test.ts` — mock fetch; verify routing by mime type; error handling.
- `AiHubComposerDropZone.test.tsx` — drop event dispatches + UI state.
- `AiHubComposer.test.tsx` — attach button opens file input; paste handler fires upload; chip appears on success; chip shows error + retry on failure.

## Task sequence (~5-6 commits)

1. `CC16 Extend CreateBinaryAssetFileToolCallback mime-type allow-list` (server, PDF + GIF).
2. `CC16 client - Add useAiHubAttachmentUpload hook`.
3. `CC16 client - Add AiHubComposerDropZone with drag-drop + paste`.
4. `CC16 client - Add attach button to composer`.
5. `CC16 client - Render in-flight upload chips + retry affordance`.
6. `CC16 Final formatting + lint fixes` (if needed).

## Commit convention

`CC16 …` / `CC16 client - …`.
