# AI Hub Phase 5B Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans.

**Goal:** Generate images and PowerPoint decks from natural language via two new hand-rolled TaskTool-pattern subagents. File viewer renders binary previews. BUILD only.

**Reference spec:** [docs/superpowers/specs/2026-04-23-ai-hub-phase5b-design.md](../specs/2026-04-23-ai-hub-phase5b-design.md).

**Depends on:** Phases 1-4 and 4.5 merged. Phase 5A not strictly required but preferred (so generative mutations are also stageable — though binary file-creation is an explicit Phase 5A exception per the 5B spec).

---

## File structure (summary)

### Server
- `tool/GenerateImageToolCallback.java` + test — wraps image provider.
- `tool/CreateBinaryAssetFileToolCallback.java` + test — saves binary content.
- `tool/CreateSlideDeckToolCallback.java` + test — produces .pptx bytes via Apache POI.
- `tool/ImageGeneratorToolCallback.java` — subagent entry point.
- `tool/SlideBuilderToolCallback.java` — subagent entry point.
- `config/ImageGeneratorConfiguration.java` + test — ChatClient + static factory.
- `config/SlideBuilderConfiguration.java` + test — ChatClient + static factory.
- `resources/prompt_image_generator.txt`.
- `resources/prompt_slide_builder.txt`.
- `CopilotConfiguration.java` — register both subagents on BUILD.
- `prompt_ai_hub_build.txt` — routing guidance.
- Possibly `AssetFileFacade` — verify/add binary creation signature; if missing, add in `server/libs/automation/automation-asset-file`.
- `build.gradle.kts` — add `org.apache.poi:poi-ooxml:5.3.0` to `ai-copilot-service` (check version catalog first).

### Client
- `AiHubFileViewer.tsx` — image + pptx rendering.
- `tests/AiHubFileViewer.test.tsx` — add image + pptx test cases.

### Commit convention
`CC5B …` / `CC5B client - …`.

---

## Task list

### Task 1: Verify image-provider facade + AssetFile binary support

**Research-only task.** Run:

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/recursing-stonebraker-2a36ca
grep -rn "interface.*ImageGeneration\|class.*ImageGeneration\|ImageModel" server/libs/platform/platform-ai/ 2>&1 | head -20
grep -rn "createFromAi\|AssetFileFacade" server/libs/automation/automation-asset-file/ 2>&1 | head -20
grep -rn "poi-ooxml" gradle/libs.versions.toml 2>&1
```

Document what's available. If `createFromAi` only takes a string, plan an additive method `createBinaryFromAi(workspaceId, name, mimeType, byte[] content, sourceOrdinal, prompt)` on the facade.

**No commit** — this is reconnaissance. Record findings in a short message to the controller so subsequent tasks know which paths exist.

### Task 2: Binary asset-file support (if needed)

Conditional on Task 1 findings. If binary isn't supported:
- Add `AssetFileFacade.createBinaryFromAi(...)` method.
- Add unit test.
- Ensure the underlying file-storage service accepts `byte[]`.

**Commit:** `CC5B Add binary createFromAi path to AssetFileFacade`

### Task 3: `CreateBinaryAssetFileToolCallback`

**Files:** `CreateBinaryAssetFileToolCallback.java` + test.

Tool name: `createBinaryAssetFile`. Input: `{filename, mimeType, base64Content, description?}`. Validates mime type against an allow-list (image/png, image/jpeg, image/webp, pptx mime). Decodes base64, calls `AssetFileFacade.createBinaryFromAi(...)`. Returns `{fileId, name, downloadUrl, sizeBytes}`.

Tests: 4 (tool-def name, happy path, invalid mime, invalid base64).

**Commit:** `CC5B Add CreateBinaryAssetFileToolCallback`

### Task 4: `GenerateImageToolCallback`

**Files:** `GenerateImageToolCallback.java` + test.

Based on Task 1 findings, constructor-inject the image-provider facade. Input: `{prompt, size?, style?}`. Output: `{imageBytes: base64, mimeType: 'image/png'}`. Tests: 3 (tool-def, happy path, provider unavailable).

**Commit:** `CC5B Add GenerateImageToolCallback`

### Task 5: `CreateSlideDeckToolCallback`

**Files:** `CreateSlideDeckToolCallback.java` + test, `build.gradle.kts`.

Add `poi-ooxml` dependency. Implement using `XMLSlideShow` (POI) — one title slide + one slide per entry. Bullet points use text-run formatting; notes go in the slide's notes page.

Input: `{title, slides: [{title, bullets[], notes?}]}`. Output: `{pptxBytes: base64, mimeType: 'application/vnd.openxmlformats-officedocument.presentationml.presentation'}`.

Test: 3-slide input → output bytes start with zip magic (`PK\x03\x04`) and contain all slide titles when unzipped.

**Commit:** `CC5B Add CreateSlideDeckToolCallback with Apache POI`

### Task 6: Image generator prompt + subagent

**Files:** `prompt_image_generator.txt`, `tool/ImageGeneratorToolCallback.java` + test, `config/ImageGeneratorConfiguration.java` + test.

System prompt tells the subagent to generate an image via `generateImage`, save via `createBinaryAssetFile`, open via `openFileTab`, and return a one-line summary.

ConfigClient wires `GenerateImageToolCallback`, `CreateBinaryAssetFileToolCallback`, `OpenFileTabToolCallback` into the subagent ChatClient. Static factory pattern (non-bean).

Tests: config tests (3, matching prior subagent patterns).

**Commit:** `CC5B Add image_generator subagent (prompt + config + callback)`

### Task 7: Slide builder prompt + subagent

**Files:** `prompt_slide_builder.txt`, `tool/SlideBuilderToolCallback.java` + test, `config/SlideBuilderConfiguration.java` + test.

System prompt: "Given a topic or source file, produce a structured outline, then call createSlideDeck, then createBinaryAssetFile, then openFileTab." Caps slide count at 15.

Config wires `GetAssetFileContentToolCallback` (to read source), `CreateSlideDeckToolCallback`, `CreateBinaryAssetFileToolCallback`, `OpenFileTabToolCallback`.

Tests: config tests (3).

**Commit:** `CC5B Add slide_builder subagent (prompt + config + callback)`

### Task 8: Register both subagents on BUILD

**Files:** `CopilotConfiguration.java`, `prompt_ai_hub_build.txt`.

In `aiHubBuildSpringAIAgent`: add `@Qualifier("imageGeneratorChatClient") ObjectProvider<ChatClient>` and `@Qualifier("slideBuilderChatClient") ObjectProvider<ChatClient>`; both `ifAvailable(...)` blocks.

Update system prompt with the `image_generator` + `slide_builder` paragraphs from the spec.

**Commit:** `CC5B Register image_generator + slide_builder subagents on BUILD`

### Task 9: Client — file viewer binary rendering

**Files:** `AiHubFileViewer.tsx` + tests.

Extend the mime-type dispatch:
- `image/*` → `<img src={downloadUrl} className="max-h-full max-w-full object-contain">` inside a centered Preview pane.
- `application/vnd.openxmlformats-officedocument.presentationml.presentation` → metadata card + prominent **Download / Open externally** button.

Both types disable the Editor/Split toggle (same treatment as unknown binary). The viewer's existing loading-state + error-state handling continues to apply.

Tests: 2 new cases — image preview renders `<img>`; pptx renders download button.

**Commit:** `CC5B client - Add image and pptx preview to file viewer`

### Task 10: Full check + manual verification

- Server tests all green.
- Client check all green.
- Manual:
  - Ask "generate a banner for X" → image file tab opens, image renders.
  - Ask "turn my spec into a 5-slide deck" → pptx file tab opens with download button.
  - Verify Phase 5A staging does NOT intercept binary file-creation (per the exception in the 5B spec).

**Commit** (if fixups needed): `CC5B Final formatting + lint fixes`.

---

## Risks

- **Image provider may not exist in EE today.** If Task 1 shows no `ImageModel` / image-provider bean, Phase 5B needs an additional sub-phase to add one — escalate to the controller. In that case, consider shipping `slide_builder` only in v5B.1 and `image_generator` in 5B.2 once an image provider is wired.
- **POI's transitive deps** can pull in old XML libs with CVEs. Use the version catalog's managed version if present; otherwise add a `dependencyResolutionManagement` constraint.
- **Base64 round-trip cost** on large images is acceptable for v5B (a few MB per image); for future video/audio we'd revisit.
