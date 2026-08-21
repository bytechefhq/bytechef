# AI Hub Phase 5B — Generative images + presentations

**Status**: Draft
**Date**: 2026-04-23
**Builds on**: Phases 1–4, 4.5, and 5A (preferred — images and pptx are mutations, Phase 5A makes them previewable and safer).
**Scope**: Close the remaining Mothership file-authoring capability gap. Add generative **image** creation and **PowerPoint** (`.pptx`) creation as two new hand-rolled `TaskTool`-pattern subagents. The file viewer gains binary preview for these new types.

---

## Goal

`createAssetFile` today handles text only — Mothership's "Files" tab can generate banners, diagrams, and slide decks. Phase 5B lets a AI Hub user say:

- "Generate a banner image for the new auth feature announcement" → an image file appears in the resource panel.
- "Turn the apollo-competitors report into a 10-slide deck" → a `.pptx` file appears.

Success in v5B: the two subagents (`image_generator`, `slide_builder`) are registered on BUILD only, the parent agent autonomously delegates when the user's request is an image or slide request, and the file viewer renders binary previews correctly (image thumbnail, pptx page-1 thumbnail).

## Non-goals (v5B)

- **Video, audio, or animated content**. Static images + static slide decks only.
- **In-browser slide / image editing**. View-only in-tab. Existing `.pptx` apps (PowerPoint, Google Slides) handle edits.
- **Multi-frame image generation** / image-to-image variants / inpainting.
- **Voice-generated presentations** (text-to-speech narration).
- **Template management**. Slide decks use a single default template in v5B.

## Architecture overview

Two new hand-rolled subagents following the Phase 3/4/4.5 pattern exactly:

- `image_generator` — wraps the existing `platform-ai` image-generation provider (already configured for workflows; find the facade).
- `slide_builder` — uses Apache POI (`poi-ooxml`) to produce `.pptx` from structured input.

Two new Spring `@Configuration` classes (`ImageGeneratorConfiguration`, `SlideBuilderConfiguration`), each exposing a `ChatClient` + a static factory for its `ToolCallback`, wired into `aiHubBuildSpringAIAgent` via `@Qualifier("imageGeneratorChatClient")` / `@Qualifier("slideBuilderChatClient")` `ObjectProvider<ChatClient>` parameters.

The subagents' tools:

- `image_generator`'s subagent `ChatClient` needs: `generateImage(prompt, style?, size?)` → returns bytes. A new `GenerateImageToolCallback` wraps the image provider. Saves to asset-files via a new `createBinaryAssetFile` callback (or extends `createAssetFile` to support binary content with a base64 input field).
- `slide_builder`'s subagent `ChatClient` needs: tools to read the user's input (text / existing markdown files via `getAssetFileContent`), plus `createSlideDeck(title, slides: [{title, bullets[], notes?}])` → returns a pptx byte array → saves via the binary asset-file path.

### Binary asset-file path

Extend asset-file infra to accept binary uploads from the copilot:

- New mime types in `CreateAssetFileToolCallback`'s allow-list OR a separate `CreateBinaryAssetFileToolCallback`: `image/png`, `image/jpeg`, `image/webp`, `application/vnd.openxmlformats-officedocument.presentationml.presentation`.
- Input schema accepts base64-encoded content for binary types (text content remains UTF-8 text for markdown/etc.). Or structure it as two separate callbacks to keep input schemas clean.
- The existing `AssetFileFacade.createFromAi(...)` signature may already accept a byte-array path — verify during planning.

Phase 5B adds binary support behind the same `openFileTab` flow — the file viewer's existing "unknown mime type" placeholder (Phase 1) is replaced with real rendering:

- `image/*` → `<img src={downloadUrl}>`
- `application/vnd.openxmlformats-officedocument.presentationml.presentation` → a page-1 thumbnail (generated server-side at creation time, stored alongside the pptx) OR a simple "Open in PowerPoint / Google Slides" placeholder for v5B.

### File viewer extension

`AiHubFileViewer.tsx` already dispatches by mime type. Add two cases:
- Image types → `<img>` element inside the existing Preview pane layout. Editor mode disabled for binary (keep the mode toggle hidden, same as unknown binary types today).
- Pptx → thumbnail (if the backend provides one) + download / "Open externally" button. Preview-only; no in-browser editing.

## Server-side design

### New Gradle dependencies

- `slide_builder` needs Apache POI:
  ```kotlin
  implementation("org.apache.poi:poi-ooxml:5.3.0")
  ```
  Check the version catalog; POI may already be pulled in transitively for another module.
- Image generation: the `platform-ai` module's existing provider(s) should suffice. No new dep.

### New Java files under `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/`

- `tool/GenerateImageToolCallback.java` — wraps the image-generation service. Tool name: `generateImage`. Input: `{prompt, size?, style?}`. Output: `{imageBytes: base64, mimeType: 'image/png'}`.
- `tool/CreateBinaryAssetFileToolCallback.java` — saves binary content as an asset file. Tool name: `createBinaryAssetFile`. Input: `{filename, mimeType, base64Content}`. Output: `{fileId, name, downloadUrl}`.
- `tool/CreateSlideDeckToolCallback.java` — converts structured slide input into pptx bytes via Apache POI. Tool name: `createSlideDeck`. Input: `{title, slides: [{title, bullets[], notes?}]}`. Output: `{pptxBytes: base64, mimeType: 'application/vnd.openxmlformats-officedocument.presentationml.presentation'}`.
- `tool/ImageGeneratorToolCallback.java` — the subagent entry point. Tool name: `image_generator`. Input: `{prompt, filename?, style?, size?}`. Delegates to the `imageGeneratorChatClient`.
- `tool/SlideBuilderToolCallback.java` — the subagent entry point. Tool name: `slide_builder`. Input: `{topic, outlineOrSourceFileId?, filename?, slideCount?}`. Delegates to `slideBuilderChatClient`.
- `config/ImageGeneratorConfiguration.java` — two beans: `imageGeneratorChatClient(...)` wired with `GenerateImageToolCallback` + `CreateBinaryAssetFileToolCallback` + `OpenFileTabToolCallback`; `static ImageGeneratorToolCallback createImageGeneratorToolCallback(...)`.
- `config/SlideBuilderConfiguration.java` — analogous, with `CreateSlideDeckToolCallback` + `CreateBinaryAssetFileToolCallback` + `GetAssetFileContentToolCallback` (to read source markdown) + `OpenFileTabToolCallback`.
- `resources/prompt_image_generator.txt` — system prompt for the subagent.
- `resources/prompt_slide_builder.txt` — system prompt.

### Modifications

- `CopilotConfiguration.aiHubBuildSpringAIAgent` — new `@Qualifier`'d `ObjectProvider<ChatClient>` params for the two new ChatClients; `ifAvailable(...)` blocks to append the corresponding ToolCallbacks.
- `prompt_ai_hub_build.txt` — add a paragraph per subagent describing when to delegate.
- Asset-file binary support (if not already there) — verify `AssetFileFacade.createFromAi` accepts `byte[]` + mime type; if it only accepts text today, add an overload or a sibling method `createBinaryFromAi(workspaceId, name, mimeType, bytes, sourceOrdinal, prompt)`.

## Client-side design

- `AiHubFileViewer.tsx` — add rendering for image and pptx mime types:
  - Image: `<img src={downloadUrl} className="max-h-full max-w-full">` wrapped in the existing Preview layout. Editor/Split modes disabled (same treatment as current binary-unknown).
  - Pptx: metadata + "Open externally" button (deep-link to a fresh download URL). Thumbnail if server provides one (can check later).
- The file-content hook (`useFileContent`) may return an empty content body for binary types — keep that path; the viewer uses `downloadUrl` directly for binary rendering.

No new routes, no new components beyond viewer additions.

## System prompt updates

Add to `prompt_ai_hub_build.txt`:
```
Image generation:
- image_generator({prompt, filename?, style?, size?}) — delegate image
  creation to a specialized subagent. The subagent generates the image via
  the platform's image provider and saves it as a workspace file. Use for
  banners, diagrams, illustrations, avatars, hero images. After the subagent
  returns, it will have already opened the file tab — summarize in one
  sentence in chat.

Slide deck generation:
- slide_builder({topic, outlineOrSourceFileId?, filename?, slideCount?}) —
  delegate .pptx creation to a specialized subagent. If outlineOrSourceFileId
  is a file id, the subagent reads that file for source material; otherwise
  it drafts from the topic. Up to 15 slides in v5B. Same file-tab flow as
  image_generator.

Prefer these subagents over trying to emit image URLs or slide JSON in chat.
```

## Testing

### Server

- `GenerateImageToolCallbackTest` — mocks the image-provider facade, asserts the tool produces the expected output shape.
- `CreateSlideDeckToolCallbackTest` — feeds a 3-slide input, asserts the output bytes start with the pptx magic number (`PK\x03\x04` zip header) and contain the slide titles (unzip in-memory and assert).
- `CreateBinaryAssetFileToolCallbackTest` — mocks `AssetFileFacade.createBinaryFromAi(...)`, verifies decoding + pass-through.
- `ImageGeneratorConfigurationTest` / `SlideBuilderConfigurationTest` — mirror the Phase 3/4/4.5 pattern (3 tests each: chat client builds, tool callback is named correctly, description mentions the capability).
- `ImageGeneratorToolCallbackTest` / `SlideBuilderToolCallbackTest` — covered indirectly through the config tests.

### Client

- `AiHubFileViewer.test.tsx` — new cases: renders image in Preview; renders pptx placeholder with download link.

## Risks and open questions

- **Image-provider availability**. Verify the `platform-ai` image generation pathway at plan-phase. If ByteChef uses a pluggable provider, the callback must degrade gracefully when no provider is configured (same `@ConditionalOnBean` pattern Phase 3 used for Firecrawl).
- **Apache POI size**. POI is heavy (~10MB). Confirm it's acceptable to add to `ai-copilot-service`; if not, a separate `ai-copilot-slide` module may be warranted.
- **Binary asset-file support on existing infra**. The existing `createFromAi` signature may not accept bytes. If not, we add a binary overload; straightforward but requires touching the asset-file module.
- **Pptx thumbnail generation**. Nice-to-have, but v5B can ship without it. Preview = download link + "Open externally" is honest and useful.
- **Interaction with Phase 5A staging**. Image/pptx creation is an `addKnowledgeBaseDocument`-like operation (additive, low-risk). The `createBinaryAssetFile` callback should **not** stage — it's in the same category as `createAssetFile` (file creation from subagents). Document this exception in the Phase 5B spec and ensure the file creation path in Phase 5A remains unstaged.

## Phase 5B+ preview

- Pptx thumbnail generation (render slide 1 to PNG at creation time).
- Template management for slide decks (multiple built-in templates, user-uploaded templates).
- Image variant iterations ("generate 3 more like that").
- Vector / diagram generation via Mermaid or similar (text-to-diagram).

## Commit convention

`CC5B …` / `CC5B client - …`.
