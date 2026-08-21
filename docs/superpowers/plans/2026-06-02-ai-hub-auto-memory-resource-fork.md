# AI Hub Auto-Memory Resource-Seam Fork — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace ByteChef's six bespoke auto-memory `ToolCallback` classes with a vendored fork of upstream `AutoMemoryTools` + `AutoMemoryToolsAdvisor` whose content read/write flows through a Spring `Resource` seam (DB-backed), keeping the `ai_auto_memory` table as the source of truth, and remove the dead reverser vaporware.

**Architecture:** A new root Gradle module `spring-ai-agent-utils/auto-memory` (mirroring the existing `spring-ai-tool-search-tool/` vendored fork) holds the two forked classes — repackaged to `com.bytechef.platform.ai.agent.memory` to avoid a classpath split with the still-needed upstream `spring-ai-agent-utils`. The fork delegates content read/write to a `MemoryResourceResolver` (Spring `Resource`/`WritableResource`) and list/delete/rename/exists to an `AutoMemoryDirectoryOps` SPI. ByteChef supplies DB-backed implementations of both over `AiAutoMemoryService`, tenant-scoped from the Spring AI `ToolContext`.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI (`spring-ai-client-chat`, `spring-ai-model`), Gradle Kotlin DSL, JUnit 5 + AssertJ + Mockito, Vitest/React (client cleanup), GraphQL.

**Spec:** `docs/superpowers/specs/2026-06-02-ai-hub-auto-memory-resource-fork-design.md`

---

## File Structure

**New vendored fork module — `spring-ai-agent-utils/` (root dir, sibling of `spring-ai-tool-search-tool/`):**
- `spring-ai-agent-utils/README.md` — provenance + removal plan
- `spring-ai-agent-utils/LICENSE.txt` — Apache 2.0 (copied from `spring-ai-tool-search-tool/LICENSE.txt`)
- `spring-ai-agent-utils/auto-memory/build.gradle.kts` — module build
- `.../auto-memory/src/main/java/com/bytechef/platform/ai/agent/memory/MemoryResourceResolver.java` — content seam (read/write)
- `.../memory/AutoMemoryDirectoryOps.java` — metadata seam (list/delete/rename/exists)
- `.../memory/AutoMemoryTools.java` — forked tools (6 `@Tool` methods, no `java.nio.Files`)
- `.../memory/AutoMemoryToolsAdvisor.java` — forked advisor (system-prompt augmentation)
- `.../auto-memory/src/main/resources/prompt/AUTO_MEMORY_TOOLS_SYSTEM_PROMPT.md` — default prompt resource
- `.../auto-memory/src/test/java/.../memory/AutoMemoryToolsTest.java`
- `.../auto-memory/src/test/java/.../memory/AutoMemoryToolsAdvisorTest.java`

**ByteChef EE — `platform-ai-hub-service` (`com.bytechef.ee.platform.aihub.tool.memory`):**
- Create: `AutoMemoryFrontmatter.java` — frontmatter render/parse codec
- Create: `DbMemoryResource.java` — `WritableResource` backed by `AiAutoMemoryService`
- Create: `DbMemoryResourceResolver.java` — `MemoryResourceResolver` impl
- Create: `DbAutoMemoryDirectoryOps.java` — `AutoMemoryDirectoryOps` impl
- Modify: `AutoMemoryToolSupport.java` — drop artifact-recording helpers
- Delete: `ReadAutoMemoryToolCallback.java`, `CreateAutoMemoryToolCallback.java`, `UpdateAutoMemoryToolCallback.java`, `DeleteAutoMemoryToolCallback.java`, `RenameAutoMemoryToolCallback.java`, `ListAutoMemoriesToolCallback.java` + their `*Test.java`
- Modify: `build.gradle.kts` — add fork dep

**ByteChef EE — `automation-ai-hub-service`:**
- Modify: `config/AiHubConfiguration.java` — wire advisor, drop `registerAutoMemoryToolCallbacks` + `buildMemoryIndexResolver` + `.memoryIndexResolver(...)`
- Create: `src/main/resources/prompt/ai_hub_auto_memory_tools_system_prompt.md` — ByteChef memory prompt
- Modify: `build.gradle.kts` — add fork dep

**Reverser cleanup (Phase 5):**
- Modify: `AiHubTaskArtifactKind.java`, `AiHubTaskArtifactStatus.java`, `EnumOrdinalStabilityTest.java`
- Modify: `ai-hub-artifact.graphqls`
- Modify (client): `graphql-types.ts` (regenerated), `tasks.api.ts`, `AiHubArtifactHistoryPage.tsx`
- Possibly remove: `AiHubSpringAIAgent.MemoryIndexResolver` interface (if no other consumers)

---

## Phase 0 — Scaffold the vendored fork module

### Task 0.1: Create module directory, LICENSE, README

**Files:**
- Create: `spring-ai-agent-utils/LICENSE.txt`
- Create: `spring-ai-agent-utils/README.md`

- [ ] **Step 1: Copy the Apache license**

Run:
```bash
mkdir -p spring-ai-agent-utils/auto-memory/src/main/java/com/bytechef/platform/ai/agent/memory
mkdir -p spring-ai-agent-utils/auto-memory/src/main/resources/prompt
mkdir -p spring-ai-agent-utils/auto-memory/src/test/java/com/bytechef/platform/ai/agent/memory
cp spring-ai-tool-search-tool/LICENSE.txt spring-ai-agent-utils/LICENSE.txt
```

- [ ] **Step 2: Write the README**

Create `spring-ai-agent-utils/README.md`:
```markdown
# spring-ai-agent-utils (vendored fork)

This directory is a **temporary, partial fork** of
[`spring-ai-community/spring-ai-agent-utils`](https://github.com/spring-ai-community/spring-ai-agent-utils).

## Why is this vendored?

ByteChef adopts upstream's Claude memory-tool surface (`AutoMemoryTools`) and its
`AutoMemoryToolsAdvisor`, but needs memory persisted in the `ai_auto_memory` database
table rather than on the filesystem. Upstream hard-wires `java.nio.file.Path` / `Files`
and cannot be extended (`protected` constructor, `private` I/O helpers). We therefore
fork **only those two classes** and change them so that:

- read + write of memory content flow through a Spring `Resource` / `WritableResource`
  seam (`MemoryResourceResolver`), and
- list / delete / rename / exists flow through an `AutoMemoryDirectoryOps` SPI.

The fork no longer references `java.nio.file.Files`.

## Repackaged, not split

The classes are repackaged from `org.springaicommunity.agent.{tools,advisors}` to
`com.bytechef.platform.ai.agent.memory`. The upstream `org.springaicommunity:spring-ai-agent-utils`
artifact stays on the classpath (other agent tools — `AskUserQuestionTool`, `FileSystemTools`,
`GrepTool`, etc. — are still consumed from it), so reusing the upstream package would create a
split package / duplicate-class hazard.

## Source provenance

- **Upstream URL**: https://github.com/spring-ai-community/spring-ai-agent-utils
- **Forked from commit**: `5548e80f5fdaa1f31a84128f5bd25ffaa2e26b40`
- **Upstream license**: Apache License 2.0 (see `LICENSE.txt`)

## Modules

| Local Gradle path | Forked upstream classes |
|---|---|
| `:spring-ai-agent-utils:auto-memory` | `AutoMemoryTools`, `AutoMemoryToolsAdvisor` |

## Removal plan

Drop this directory and restore the upstream `AutoMemoryTools`/`AutoMemoryToolsAdvisor`
once upstream exposes a pluggable, non-filesystem storage backend (a store interface
upstream of the `Path`/`Files` calls). The DB-backed implementations
(`MemoryResourceResolver`, `AutoMemoryDirectoryOps`) live in ByteChef's
`platform-ai-hub-service` and would be re-pointed at the upstream extension point.
```

- [ ] **Step 3: Commit**

```bash
git add spring-ai-agent-utils/LICENSE.txt spring-ai-agent-utils/README.md
git commit -m "0_732 Scaffold spring-ai-agent-utils vendored fork (license, README)"
```

### Task 0.2: Module build file + settings include

**Files:**
- Create: `spring-ai-agent-utils/auto-memory/build.gradle.kts`
- Modify: `settings.gradle.kts:30` (after the `spring-ai-tool-search-tool` includes)

- [ ] **Step 1: Write the build file**

Create `spring-ai-agent-utils/auto-memory/build.gradle.kts`:
```kotlin
plugins {
    id("com.bytechef.java-library-conventions")
}

val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

// Temporary, partial vendored fork of org.springaicommunity:spring-ai-agent-utils —
// only AutoMemoryTools + AutoMemoryToolsAdvisor, repackaged to com.bytechef.platform.ai.agent.memory
// and re-backed by a Spring Resource seam (DB storage) instead of java.nio.Files.
// See ../README.md for source provenance and the removal plan.
dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:${libs.findVersion("spring-ai").get()}"))
    api("org.springframework.ai:spring-ai-client-chat")
    api("org.springframework.ai:spring-ai-model")
    implementation("org.springframework:spring-core")
    implementation("org.slf4j:slf4j-api")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
```

- [ ] **Step 2: Register the module in settings.gradle.kts**

In `settings.gradle.kts`, add after line 30 (`include("spring-ai-tool-search-tool:tool-searcher-vectorstore")`):
```kotlin
include("spring-ai-agent-utils:auto-memory")
```

- [ ] **Step 3: Verify the empty module configures**

Run: `./gradlew :spring-ai-agent-utils:auto-memory:help -q`
Expected: BUILD SUCCESSFUL (module is recognized; no sources yet).

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts spring-ai-agent-utils/auto-memory/build.gradle.kts
git commit -m "0_732 Register spring-ai-agent-utils:auto-memory Gradle module"
```

---

## Phase 1 — Fork the seams + AutoMemoryTools

### Task 1.1: Define the two seam interfaces

**Files:**
- Create: `spring-ai-agent-utils/auto-memory/src/main/java/com/bytechef/platform/ai/agent/memory/MemoryResourceResolver.java`
- Create: `.../memory/AutoMemoryDirectoryOps.java`

- [ ] **Step 1: Write `MemoryResourceResolver`**

```java
/*
 * Copyright 2025 - 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bytechef.platform.ai.agent.memory;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.io.WritableResource;

/**
 * Resolves a relative memory path to a tenant-scoped, writable {@link org.springframework.core.io.Resource}. This is
 * the read/write content seam for {@link AutoMemoryTools}: reads use {@code getInputStream()} and writes use
 * {@code getOutputStream()}. Implementations derive the tenant from the supplied {@link ToolContext}.
 */
@FunctionalInterface
public interface MemoryResourceResolver {

    WritableResource resolve(String relativePath, ToolContext toolContext);
}
```

- [ ] **Step 2: Write `AutoMemoryDirectoryOps`**

```java
/*
 * Copyright 2025 - 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bytechef.platform.ai.agent.memory;

import org.springframework.ai.chat.model.ToolContext;

/**
 * Metadata seam for {@link AutoMemoryTools}: listing the memory index, existence checks, deletion, and renaming.
 * Replaces upstream's {@code java.nio.file.Files}-based directory operations so a non-filesystem backend (e.g. a
 * database) can serve them. Implementations derive the tenant from the supplied {@link ToolContext}.
 */
public interface AutoMemoryDirectoryOps {

    /**
     * Renders the memory index. {@code path} is the root ("", "/" or "MEMORY.md"); a human-readable index listing is
     * returned.
     */
    String list(String path, ToolContext toolContext);

    boolean exists(String relativePath, ToolContext toolContext);

    void delete(String relativePath, ToolContext toolContext);

    void rename(String oldRelativePath, String newRelativePath, ToolContext toolContext);
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew :spring-ai-agent-utils:auto-memory:compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add spring-ai-agent-utils/auto-memory/src/main/java/com/bytechef/platform/ai/agent/memory/MemoryResourceResolver.java \
        spring-ai-agent-utils/auto-memory/src/main/java/com/bytechef/platform/ai/agent/memory/AutoMemoryDirectoryOps.java
git commit -m "0_732 Add AutoMemoryTools storage seams (Resource + directory SPI)"
```

### Task 1.2: Write the failing test for AutoMemoryTools

**Files:**
- Create: `spring-ai-agent-utils/auto-memory/src/test/java/com/bytechef/platform/ai/agent/memory/AutoMemoryToolsTest.java`

- [ ] **Step 1: Write the test (with in-memory fakes)**

```java
/*
 * Copyright 2025 - 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bytechef.platform.ai.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.WritableResource;

class AutoMemoryToolsTest {

    private final Map<String, String> store = new LinkedHashMap<>();
    private AutoMemoryTools autoMemoryTools;

    @BeforeEach
    void setUp() {
        store.clear();

        MemoryResourceResolver resolver = (relativePath, toolContext) -> new FakeMemoryResource(relativePath);

        AutoMemoryDirectoryOps directoryOps = new AutoMemoryDirectoryOps() {
            @Override
            public String list(String path, ToolContext toolContext) {
                if (store.isEmpty()) {
                    return "No memories yet.";
                }

                StringBuilder stringBuilder = new StringBuilder("MEMORY index:\n");

                for (String name : store.keySet()) {
                    stringBuilder.append("- ")
                        .append(name)
                        .append("\n");
                }

                return stringBuilder.toString();
            }

            @Override
            public boolean exists(String relativePath, ToolContext toolContext) {
                return store.containsKey(relativePath);
            }

            @Override
            public void delete(String relativePath, ToolContext toolContext) {
                store.remove(relativePath);
            }

            @Override
            public void rename(String oldRelativePath, String newRelativePath, ToolContext toolContext) {
                store.put(newRelativePath, store.remove(oldRelativePath));
            }
        };

        autoMemoryTools = new AutoMemoryTools(resolver, directoryOps);
    }

    @Test
    void testMemoryCreateThenViewRoundTrips() {
        String createResult = autoMemoryTools.memoryCreate("user_profile.md", "hello world", null);

        assertThat(createResult).contains("Successfully created");
        assertThat(store).containsKey("user_profile.md");

        String viewResult = autoMemoryTools.memoryView("user_profile.md", null, null);

        assertThat(viewResult).contains("hello world");
    }

    @Test
    void testMemoryCreateRejectsExistingFile() {
        store.put("user_profile.md", "existing");

        String result = autoMemoryTools.memoryCreate("user_profile.md", "new", null);

        assertThat(result).contains("already exists");
    }

    @Test
    void testMemoryViewOnRootListsIndex() {
        store.put("user_profile.md", "x");

        String result = autoMemoryTools.memoryView("", null, null);

        assertThat(result).contains("MEMORY index");
        assertThat(result).contains("user_profile.md");
    }

    @Test
    void testMemoryStrReplaceEditsContent() {
        store.put("note.md", "the quick brown fox");

        String result = autoMemoryTools.memoryStrReplace("note.md", "brown", "red", null);

        assertThat(result).contains("Successfully edited");
        assertThat(store.get("note.md")).isEqualTo("the quick red fox");
    }

    @Test
    void testMemoryStrReplaceRejectsNonUniqueMatch() {
        store.put("note.md", "ab ab");

        String result = autoMemoryTools.memoryStrReplace("note.md", "ab", "x", null);

        assertThat(result).contains("appears 2 times");
        assertThat(store.get("note.md")).isEqualTo("ab ab");
    }

    @Test
    void testMemoryInsertAddsLine() {
        store.put("note.md", "line1\nline2");

        String result = autoMemoryTools.memoryInsert("note.md", 1, "inserted", null);

        assertThat(result).contains("Successfully inserted");
        assertThat(store.get("note.md")).isEqualTo("line1\ninserted\nline2");
    }

    @Test
    void testMemoryDeleteRemovesFile() {
        store.put("note.md", "x");

        String result = autoMemoryTools.memoryDelete("note.md", null);

        assertThat(result).contains("Successfully deleted");
        assertThat(store).doesNotContainKey("note.md");
    }

    @Test
    void testMemoryRenameMovesFile() {
        store.put("old.md", "x");

        String result = autoMemoryTools.memoryRename("old.md", "new.md", null);

        assertThat(result).contains("Successfully renamed");
        assertThat(store).doesNotContainKey("old.md");
        assertThat(store).containsKey("new.md");
    }

    /** A WritableResource backed by the test's in-memory {@code store} map. */
    private final class FakeMemoryResource extends AbstractResource implements WritableResource {

        private final String name;

        private FakeMemoryResource(String name) {
            this.name = name;
        }

        @Override
        public String getDescription() {
            return "fake:" + name;
        }

        @Override
        public boolean exists() {
            return store.containsKey(name);
        }

        @Override
        public InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(
                store.getOrDefault(name, "")
                    .getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream() {
                @Override
                public void close() throws IOException {
                    super.close();

                    store.put(name, toString(StandardCharsets.UTF_8));
                }
            };
        }
    }
}
```

- [ ] **Step 2: Run to verify it fails to compile (AutoMemoryTools not yet written)**

Run: `./gradlew :spring-ai-agent-utils:auto-memory:test --tests "*AutoMemoryToolsTest" -q`
Expected: FAIL — compilation error, `AutoMemoryTools` symbol not found (and no `memoryCreate/memoryView/...` methods).

### Task 1.3: Implement the forked AutoMemoryTools

**Files:**
- Create: `spring-ai-agent-utils/auto-memory/src/main/java/com/bytechef/platform/ai/agent/memory/AutoMemoryTools.java`

- [ ] **Step 1: Write the implementation**

```java
/*
 * Copyright 2025 - 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bytechef.platform.ai.agent.memory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Tools for managing persistent memory entries. Forked from
 * {@code org.springaicommunity.agent.tools.AutoMemoryTools} (commit 5548e80) and re-backed by a
 * {@link MemoryResourceResolver} (content read/write) plus an {@link AutoMemoryDirectoryOps} SPI
 * (list/delete/rename/exists), so memory can live outside the filesystem. The LLM-facing tool surface mirrors the
 * Claude memory-tool spec: view, create, str_replace, insert, delete, rename.
 */
public class AutoMemoryTools {

    private final MemoryResourceResolver resourceResolver;
    private final AutoMemoryDirectoryOps directoryOps;

    public AutoMemoryTools(MemoryResourceResolver resourceResolver, AutoMemoryDirectoryOps directoryOps) {
        Assert.notNull(resourceResolver, "resourceResolver must not be null");
        Assert.notNull(directoryOps, "directoryOps must not be null");

        this.resourceResolver = resourceResolver;
        this.directoryOps = directoryOps;
    }

    // @formatter:off
    @Tool(name = "MemoryView", description = """
        View a memory entry, or list the memory index.

        Usage:
        - Use an empty path, "/" or "MEMORY.md" to read the always-loaded index of all memory entries.
          Consult it before reading or writing any memory.
        - Otherwise path names a single memory entry; its contents are returned with line numbers.
        - Optionally supply a line range 'start,end' to page through large entries.
        """)
    public String memoryView(
        @ToolParam(description = "Memory entry path, or empty/'/'/'MEMORY.md' for the index.") String path,
        @ToolParam(description = "Optional line range 'start,end' (e.g. '1,50') when viewing an entry.",
            required = false) String viewRange,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return directoryOps.list(path == null ? "" : path, toolContext);
            }

            if (!directoryOps.exists(path, toolContext)) {
                return "Error: Path does not exist: " + path;
            }

            String content = read(path, toolContext);

            return formatFileView(path, content, viewRange);
        } catch (IOException exception) {
            return "Error reading path: " + exception.getMessage();
        } catch (RuntimeException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    // @formatter:off
    @Tool(name = "MemoryCreate", description = """
        Create a new memory entry.

        Usage:
        - The entry must NOT already exist; use MemoryStrReplace to update an existing entry.
        - Provide the full entry content, including the YAML frontmatter block followed by the body.
        - After creating an entry, the index (MEMORY.md) updates automatically — you do not edit it by hand.
        - Always check the index (MemoryView on MEMORY.md) first to avoid duplicates.
        """)
    public String memoryCreate(
        @ToolParam(description = "Path for the new entry (e.g. 'user_profile.md').") String path,
        @ToolParam(description = "Full entry content: YAML frontmatter block then the body.") String fileText,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index is maintained automatically and cannot be created directly.";
            }

            if (directoryOps.exists(path, toolContext)) {
                return "Error: File already exists: " + path + ". Use MemoryStrReplace to modify existing files.";
            }

            String text = fileText != null ? fileText : "";

            write(path, text, toolContext);

            return "Successfully created file: " + path + " (" + text.length() + " bytes)";
        } catch (IOException exception) {
            return "Error creating file: " + exception.getMessage();
        } catch (RuntimeException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    // @formatter:off
    @Tool(name = "MemoryStrReplace", description = """
        Replace an exact string in an existing memory entry.

        Usage:
        - old_str must match exactly (including whitespace and newlines) and must appear exactly once.
        - If old_str appears more than once the edit is rejected — include more surrounding context.
        - new_str can be empty to delete the matched text.
        """)
    public String memoryStrReplace(
        @ToolParam(description = "Path of the entry to edit.") String path,
        @ToolParam(description = "Exact text to find; must appear exactly once.") String oldStr,
        @ToolParam(description = "Replacement text; empty to delete the matched text.") String newStr,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index is maintained automatically and cannot be edited directly.";
            }

            if (!directoryOps.exists(path, toolContext)) {
                return "Error: File does not exist: " + path;
            }

            String content = read(path, toolContext);
            int occurrences = countOccurrences(content, oldStr);

            if (occurrences == 0) {
                return "Error: old_str not found in file: " + path;
            }

            if (occurrences > 1) {
                return String.format(
                    "Error: old_str appears %d times in the file. Provide more surrounding context to make it unique.",
                    occurrences);
            }

            String replacement = newStr != null ? newStr : "";
            String updated = replaceFirst(content, oldStr, replacement);

            write(path, updated, toolContext);

            if (!StringUtils.hasText(replacement)) {
                return String.format("Successfully deleted matched text from %s.", path);
            }

            return String.format(
                "Successfully edited %s. Here's a snippet of the result:%n%s", path,
                generateEditSnippet(updated, replacement));
        } catch (IOException exception) {
            return "Error editing file: " + exception.getMessage();
        } catch (RuntimeException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    // @formatter:off
    @Tool(name = "MemoryInsert", description = """
        Insert text at a specific line number in an existing memory entry.

        Usage:
        - insert_line is the line number AFTER which the new text is inserted (0 inserts at the beginning).
        - Lines are 1-indexed. Providing insert_line equal to the total line count appends to the end.
        """)
    public String memoryInsert(
        @ToolParam(description = "Path of the entry to modify.") String path,
        @ToolParam(description = "Line number after which to insert (0 = before first line).") Integer insertLine,
        @ToolParam(description = "Text to insert.") String insertText,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index is maintained automatically and cannot be edited directly.";
            }

            if (!directoryOps.exists(path, toolContext)) {
                return "Error: File does not exist: " + path;
            }

            if (insertLine == null || insertLine < 0) {
                return "Error: insert_line must be a non-negative integer";
            }

            String content = read(path, toolContext);
            boolean trailingNewline = content.endsWith("\n");

            List<String> lines = new ArrayList<>(List.of(content.isEmpty() ? new String[0] : content.split("\n", -1)));

            if (trailingNewline && !lines.isEmpty()) {
                lines.remove(lines.size() - 1);
            }

            if (insertLine > lines.size()) {
                return String.format("Error: insert_line %d exceeds file length of %d lines", insertLine, lines.size());
            }

            lines.add(insertLine, insertText != null ? insertText : "");

            String updated = String.join("\n", lines) + (trailingNewline ? "\n" : "");

            write(path, updated, toolContext);

            return "Successfully inserted text at line " + insertLine + " in: " + path;
        } catch (IOException exception) {
            return "Error inserting into file: " + exception.getMessage();
        } catch (RuntimeException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    // @formatter:off
    @Tool(name = "MemoryDelete", description = """
        Delete a memory entry.

        Usage:
        - This operation is irreversible; use with caution.
        - The index (MEMORY.md) updates automatically after deletion.
        - Use when a memory is confirmed stale, wrong, or superseded.
        """)
    public String memoryDelete(
        @ToolParam(description = "Path of the entry to delete.") String path,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index cannot be deleted.";
            }

            if (!directoryOps.exists(path, toolContext)) {
                return "Error: Path does not exist: " + path;
            }

            directoryOps.delete(path, toolContext);

            return "Successfully deleted file: " + path;
        } catch (RuntimeException exception) {
            return "Error deleting path: " + exception.getMessage();
        }
    }

    // @formatter:off
    @Tool(name = "MemoryRename", description = """
        Rename a memory entry.

        Usage:
        - The source entry must exist; the destination must NOT already exist.
        - The index (MEMORY.md) updates automatically after the rename.
        """)
    public String memoryRename(
        @ToolParam(description = "Current path of the entry.") String oldPath,
        @ToolParam(description = "New path for the entry.") String newPath,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(oldPath) || isIndexOrRoot(newPath)) {
                return "Error: The index cannot be renamed.";
            }

            if (!directoryOps.exists(oldPath, toolContext)) {
                return "Error: Source path does not exist: " + oldPath;
            }

            if (directoryOps.exists(newPath, toolContext)) {
                return "Error: Destination path already exists: " + newPath;
            }

            directoryOps.rename(oldPath, newPath, toolContext);

            return String.format("Successfully renamed '%s' to '%s'", oldPath, newPath);
        } catch (RuntimeException exception) {
            return "Error renaming path: " + exception.getMessage();
        }
    }

    private String read(String path, ToolContext toolContext) throws IOException {
        try (InputStream inputStream = resourceResolver.resolve(path, toolContext)
            .getInputStream()) {

            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private void write(String path, String content, ToolContext toolContext) throws IOException {
        WritableResource resource = resourceResolver.resolve(path, toolContext);

        try (OutputStream outputStream = resource.getOutputStream()) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static boolean isIndexOrRoot(String path) {
        return !StringUtils.hasText(path) || path.equals("/") || path.equals("MEMORY.md");
    }

    private static String formatFileView(String path, String content, String viewRange) {
        String[] allLines = content.split("\n", -1);
        int totalLines = allLines.length;

        int startLine = 1;
        int endLine = totalLines;

        if (StringUtils.hasText(viewRange)) {
            String[] parts = viewRange.split(",");

            if (parts.length != 2) {
                return "Error: view_range must be 'start,end' (e.g. '1,50')";
            }

            try {
                startLine = Math.max(1, Integer.parseInt(parts[0].trim()));
                endLine = Math.min(totalLines, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException exception) {
                return "Error: view_range must be 'start,end' integers (e.g. '1,50')";
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format("File: %s%nLines %d-%d of %d%n%n", path, startLine, endLine, totalLines));

        for (int index = startLine - 1; index < endLine; index++) {
            stringBuilder.append(String.format("%6d\t%s%n", index + 1, allLines[index]));
        }

        return stringBuilder.toString();
    }

    private static int countOccurrences(String text, String substring) {
        if (!StringUtils.hasLength(substring)) {
            return 0;
        }

        int count = 0;
        int index = 0;

        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }

        return count;
    }

    private static String replaceFirst(String text, String oldStr, String newStr) {
        int index = text.indexOf(oldStr);

        if (index == -1) {
            return text;
        }

        return text.substring(0, index) + newStr + text.substring(index + oldStr.length());
    }

    private static String generateEditSnippet(String fileContent, String newStr) {
        String[] lines = fileContent.split("\n", -1);
        String[] newLines = newStr.split("\n", -1);

        int editStartLine = -1;
        int editEndLine = -1;

        for (int i = 0; i < lines.length; i++) {
            if (newLines.length > 0 && lines[i].contains(newLines[0])) {
                boolean matches = true;

                for (int j = 1; j < newLines.length && i + j < lines.length; j++) {
                    if (!lines[i + j].contains(newLines[j])) {
                        matches = false;

                        break;
                    }
                }

                if (matches) {
                    editStartLine = i;
                    editEndLine = i + newLines.length - 1;

                    break;
                }
            }
        }

        if (editStartLine == -1) {
            editStartLine = 0;
            editEndLine = Math.min(10, lines.length - 1);
        }

        int startLine = Math.max(0, editStartLine - 5);
        int endLine = Math.min(lines.length - 1, editEndLine + 5);

        StringBuilder snippet = new StringBuilder();

        for (int i = startLine; i <= endLine; i++) {
            snippet.append(String.format("%6d→%s", i + 1, lines[i]));

            if (i < endLine) {
                snippet.append("\n");
            }
        }

        return snippet.toString();
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :spring-ai-agent-utils:auto-memory:test --tests "*AutoMemoryToolsTest" -q`
Expected: PASS (8 tests).

- [ ] **Step 3: Apply formatting**

Run: `./gradlew :spring-ai-agent-utils:auto-memory:spotlessApply -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add spring-ai-agent-utils/auto-memory/src/main/java/com/bytechef/platform/ai/agent/memory/AutoMemoryTools.java \
        spring-ai-agent-utils/auto-memory/src/test/java/com/bytechef/platform/ai/agent/memory/AutoMemoryToolsTest.java
git commit -m "0_732 Add forked AutoMemoryTools backed by Resource + directory SPI"
```

---

## Phase 2 — Fork AutoMemoryToolsAdvisor

### Task 2.1: Default prompt resource

**Files:**
- Create: `spring-ai-agent-utils/auto-memory/src/main/resources/prompt/AUTO_MEMORY_TOOLS_SYSTEM_PROMPT.md`

- [ ] **Step 1: Write a minimal default prompt** (ByteChef overrides this at wiring time, but the advisor needs a non-null default to construct)

```markdown
You have a persistent long-term memory accessed through the Memory* tools (MemoryView,
MemoryCreate, MemoryStrReplace, MemoryInsert, MemoryDelete, MemoryRename).

Start by viewing the index (MemoryView on "MEMORY.md") to see what you already remember.
Create or update memory entries when the user shares durable facts (preferences, decisions,
references). The index updates automatically; you do not edit it by hand.
```

- [ ] **Step 2: Commit**

```bash
git add spring-ai-agent-utils/auto-memory/src/main/resources/prompt/AUTO_MEMORY_TOOLS_SYSTEM_PROMPT.md
git commit -m "0_732 Add default memory-tools system prompt resource"
```

### Task 2.2: Write the failing advisor test

**Files:**
- Create: `spring-ai-agent-utils/auto-memory/src/test/java/com/bytechef/platform/ai/agent/memory/AutoMemoryToolsAdvisorTest.java`

- [ ] **Step 1: Write the test**

```java
/*
 * Copyright 2025 - 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bytechef.platform.ai.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;

class AutoMemoryToolsAdvisorTest {

    private final AutoMemoryTools autoMemoryTools = new AutoMemoryTools(
        (relativePath, toolContext) -> {
            throw new UnsupportedOperationException();
        },
        new AutoMemoryDirectoryOps() {
            @Override
            public String list(String path, org.springframework.ai.chat.model.ToolContext toolContext) {
                return "";
            }

            @Override
            public boolean exists(String relativePath, org.springframework.ai.chat.model.ToolContext toolContext) {
                return false;
            }

            @Override
            public void delete(String relativePath, org.springframework.ai.chat.model.ToolContext toolContext) {
            }

            @Override
            public void rename(
                String oldRelativePath, String newRelativePath,
                org.springframework.ai.chat.model.ToolContext toolContext) {
            }
        });

    @Test
    void testBeforeAugmentsSystemPromptAndAddsToolCallbacks() {
        AutoMemoryToolsAdvisor advisor = AutoMemoryToolsAdvisor.builder()
            .autoMemoryTools(autoMemoryTools)
            .memorySystemPrompt(new ByteArrayResource("REMEMBER THINGS".getBytes()))
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(
                List.of(new SystemMessage("base system")),
                ToolCallingChatOptions.builder()
                    .build()))
            .build();

        ChatClientRequest result = advisor.before(request, null);

        String systemText = result.prompt()
            .getSystemMessage()
            .getText();

        assertThat(systemText).contains("base system");
        assertThat(systemText).contains("REMEMBER THINGS");

        List<ToolCallback> toolCallbacks =
            ((ToolCallingChatOptions) result.prompt()
                .getOptions()).getToolCallbacks();

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .contains("MemoryView", "MemoryCreate", "MemoryStrReplace", "MemoryInsert", "MemoryDelete", "MemoryRename");
    }
}
```

- [ ] **Step 2: Run to verify it fails (advisor not written)**

Run: `./gradlew :spring-ai-agent-utils:auto-memory:test --tests "*AutoMemoryToolsAdvisorTest" -q`
Expected: FAIL — `AutoMemoryToolsAdvisor` symbol not found.

### Task 2.3: Implement the forked advisor

**Files:**
- Create: `spring-ai-agent-utils/auto-memory/src/main/java/com/bytechef/platform/ai/agent/memory/AutoMemoryToolsAdvisor.java`

- [ ] **Step 1: Write the implementation** (forked from upstream; builder takes the `AutoMemoryTools` instance instead of a `memoriesRootDirectory`)

```java
/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bytechef.platform.ai.agent.memory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Forked from {@code org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor} (commit 5548e80). Augments the system
 * message with a memory system prompt (and an optional consolidation reminder) and registers the {@link AutoMemoryTools}
 * tool callbacks. Unlike upstream, the builder takes a pre-constructed {@link AutoMemoryTools} instance (whose storage
 * backend is supplied by the caller) instead of a filesystem {@code memoriesRootDirectory}.
 */
public class AutoMemoryToolsAdvisor implements BaseChatMemoryAdvisor {

    private final int order;
    private final String memorySystemPrompt;
    private final List<ToolCallback> memoryToolCallbacks;
    private final BiPredicate<ChatClientRequest, Instant> memoryConsolidationTrigger;

    private AutoMemoryToolsAdvisor(
        int order, String memorySystemPrompt, List<ToolCallback> memoryToolCallbacks,
        BiPredicate<ChatClientRequest, Instant> memoryConsolidationTrigger) {

        this.order = order;
        this.memorySystemPrompt = memorySystemPrompt;
        this.memoryToolCallbacks = memoryToolCallbacks;
        this.memoryConsolidationTrigger = memoryConsolidationTrigger;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        if (!(chatClientRequest.prompt()
            .getOptions() instanceof ToolCallingChatOptions toolOptions)) {

            return chatClientRequest;
        }

        String consolidationReminder = memoryConsolidationTrigger.test(chatClientRequest, Instant.now())
            ? "<system-reminder>Consolidate the long-term memory by summarizing and removing redundant "
                + "information.</system-reminder>"
            : "";

        Prompt augmentedPrompt = chatClientRequest.prompt()
            .augmentSystemMessage(chatClientRequest.prompt()
                .getSystemMessage()
                .getText() + System.lineSeparator() + System.lineSeparator() + memorySystemPrompt
                + System.lineSeparator() + System.lineSeparator() + consolidationReminder);

        ToolCallingChatOptions toolOptionsCopy = toolOptions.copy();

        List<ToolCallback> toolCallbacks = new ArrayList<>(toolOptionsCopy.getToolCallbacks());

        Set<String> existingNames = toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toSet());

        memoryToolCallbacks.stream()
            .filter(toolCallback -> !existingNames.contains(toolCallback.getToolDefinition()
                .name()))
            .forEach(toolCallbacks::add);

        ToolCallingChatOptions mergedOptions = ((ToolCallingChatOptions.Builder<?>) toolOptionsCopy.mutate())
            .toolCallbacks(new ArrayList<>(toolCallbacks))
            .build();

        return chatClientRequest.mutate()
            .prompt(augmentedPrompt.mutate()
                .chatOptions(mergedOptions)
                .build())
            .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        // Before the default ToolCallingAdvisor which is at HIGHEST_PRECEDENCE + 300.
        private int order = BaseAdvisor.HIGHEST_PRECEDENCE + 200;
        private AutoMemoryTools autoMemoryTools;
        private Resource memorySystemPrompt;
        private BiPredicate<ChatClientRequest, Instant> memoryConsolidationTrigger = (request, instant) -> false;

        private Builder() {
        }

        public Builder order(int order) {
            this.order = order;

            return this;
        }

        public Builder autoMemoryTools(AutoMemoryTools autoMemoryTools) {
            this.autoMemoryTools = autoMemoryTools;

            return this;
        }

        public Builder memorySystemPrompt(Resource memorySystemPrompt) {
            Assert.notNull(memorySystemPrompt, "Memory system prompt must not be null");

            this.memorySystemPrompt = memorySystemPrompt;

            return this;
        }

        public Builder memoryConsolidationTrigger(
            BiPredicate<ChatClientRequest, Instant> memoryConsolidationTrigger) {

            Assert.notNull(memoryConsolidationTrigger, "Memory consolidation trigger must not be null");

            this.memoryConsolidationTrigger = memoryConsolidationTrigger;

            return this;
        }

        public AutoMemoryToolsAdvisor build() {
            Assert.notNull(this.autoMemoryTools, "autoMemoryTools must not be null");
            Assert.notNull(this.memorySystemPrompt, "Memory system prompt must not be null");

            List<ToolCallback> memoryToolCallbacks = Arrays.asList(
                MethodToolCallbackProvider.builder()
                    .toolObjects(this.autoMemoryTools)
                    .build()
                    .getToolCallbacks());

            String memorySystemPromptText = PromptTemplate.builder()
                .resource(this.memorySystemPrompt)
                .build()
                .render();

            return new AutoMemoryToolsAdvisor(
                this.order, memorySystemPromptText, memoryToolCallbacks, this.memoryConsolidationTrigger);
        }
    }
}
```

> Note: upstream rendered the prompt with a `MEMORIES_ROOT_DIERCTORY` template variable. Since there is no
> filesystem root in this fork, the prompt is rendered with no variables. If a ByteChef prompt contains literal
> `{...}` braces, `PromptTemplate` will try to resolve them — keep the ByteChef prompt free of unescaped braces
> (Task 4.4).

- [ ] **Step 2: Run the advisor test to verify it passes**

Run: `./gradlew :spring-ai-agent-utils:auto-memory:test --tests "*AutoMemoryToolsAdvisorTest" -q`
Expected: PASS.

- [ ] **Step 3: Run the whole module + format**

Run: `./gradlew :spring-ai-agent-utils:auto-memory:spotlessApply :spring-ai-agent-utils:auto-memory:test -q`
Expected: BUILD SUCCESSFUL (9 tests).

- [ ] **Step 4: Commit**

```bash
git add spring-ai-agent-utils/auto-memory/src/main/java/com/bytechef/platform/ai/agent/memory/AutoMemoryToolsAdvisor.java \
        spring-ai-agent-utils/auto-memory/src/test/java/com/bytechef/platform/ai/agent/memory/AutoMemoryToolsAdvisorTest.java
git commit -m "0_732 Add forked AutoMemoryToolsAdvisor (takes AutoMemoryTools instance)"
```

---

## Phase 3 — ByteChef DB-backed seam implementations

All files in this phase live in
`server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/memory/`
and use the **ByteChef Enterprise license header** + `@version ee` Javadoc tag.

### Task 3.1: Add the fork dependency to platform-ai-hub-service

**Files:**
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/build.gradle.kts`

- [ ] **Step 1: Add the dependency** (next to the existing tool-search vendored-fork deps, around line 5)

```kotlin
    implementation(project(":spring-ai-agent-utils:auto-memory"))
```

- [ ] **Step 2: Verify it resolves**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/build.gradle.kts
git commit -m "0_732 Depend platform-ai-hub-service on spring-ai-agent-utils:auto-memory"
```

### Task 3.2: Frontmatter codec — failing test

**Files:**
- Create: `.../memory/AutoMemoryFrontmatterTest.java`

- [ ] **Step 1: Write the test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.aihub.tool.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import org.junit.jupiter.api.Test;

class AutoMemoryFrontmatterTest {

    @Test
    void testParseExtractsFieldsAndBody() {
        String text = """
            ---
            name: user_profile
            title: User Profile
            description: who the user is
            type: USER
            ---
            The user prefers tabs.""";

        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse(text);

        assertThat(parsed.title()).isEqualTo("User Profile");
        assertThat(parsed.description()).isEqualTo("who the user is");
        assertThat(parsed.memoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(parsed.content()).isEqualTo("The user prefers tabs.");
    }

    @Test
    void testParseToleratesMissingFrontmatter() {
        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse("just a body, no frontmatter");

        assertThat(parsed.title()).isNull();
        assertThat(parsed.description()).isNull();
        assertThat(parsed.memoryType()).isNull();
        assertThat(parsed.content()).isEqualTo("just a body, no frontmatter");
    }

    @Test
    void testRenderRoundTripsThroughParse() {
        String rendered = AutoMemoryFrontmatter.render(
            "user_profile", "User Profile", "who the user is", AiAutoMemoryType.USER, "The user prefers tabs.");

        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse(rendered);

        assertThat(parsed.title()).isEqualTo("User Profile");
        assertThat(parsed.description()).isEqualTo("who the user is");
        assertThat(parsed.memoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(parsed.content()).isEqualTo("The user prefers tabs.");
    }

    @Test
    void testRenderOmitsBlankDescription() {
        String rendered = AutoMemoryFrontmatter.render(
            "x", "X", null, AiAutoMemoryType.PROJECT, "body");

        assertThat(rendered).doesNotContain("description:");
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test --tests "*AutoMemoryFrontmatterTest" -q`
Expected: FAIL — `AutoMemoryFrontmatter` not found.

### Task 3.3: Frontmatter codec — implementation

**Files:**
- Create: `.../memory/AutoMemoryFrontmatter.java`

- [ ] **Step 1: Write the implementation**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.aihub.tool.memory;

import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import org.jspecify.annotations.Nullable;

/**
 * Renders an {@link com.bytechef.platform.ai.auto.memory.AiAutoMemory} as a frontmatter document and parses such a
 * document back into its fields. The on-the-wire form the LLM sees through the Memory* tools is:
 *
 * <pre>
 * ---
 * name: &lt;slug&gt;
 * title: &lt;title&gt;
 * description: &lt;description&gt;   (omitted when blank)
 * type: &lt;USER|FEEDBACK|PROJECT|REFERENCE&gt;
 * ---
 * &lt;body&gt;
 * </pre>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AutoMemoryFrontmatter {

    private static final String DELIMITER = "---";

    private AutoMemoryFrontmatter() {
    }

    record Parsed(
        @Nullable String title, @Nullable String description, @Nullable AiAutoMemoryType memoryType, String content) {
    }

    static String render(
        String name, String title, @Nullable String description, AiAutoMemoryType memoryType, String content) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(DELIMITER)
            .append("\n")
            .append("name: ")
            .append(name)
            .append("\n")
            .append("title: ")
            .append(title)
            .append("\n");

        if (description != null && !description.isBlank()) {
            stringBuilder.append("description: ")
                .append(description)
                .append("\n");
        }

        stringBuilder.append("type: ")
            .append(memoryType.name())
            .append("\n")
            .append(DELIMITER)
            .append("\n")
            .append(content);

        return stringBuilder.toString();
    }

    static Parsed parse(String text) {
        String normalized = text == null ? "" : text;

        if (!normalized.stripLeading()
            .startsWith(DELIMITER)) {

            return new Parsed(null, null, null, normalized);
        }

        String afterFirst = normalized.stripLeading()
            .substring(DELIMITER.length());

        int closingIndex = afterFirst.indexOf("\n" + DELIMITER);

        if (closingIndex < 0) {
            return new Parsed(null, null, null, normalized);
        }

        String frontmatter = afterFirst.substring(0, closingIndex);
        String afterClosing = afterFirst.substring(closingIndex + ("\n" + DELIMITER).length());
        String content = afterClosing.startsWith("\n") ? afterClosing.substring(1) : afterClosing.stripLeading();

        String title = null;
        String description = null;
        AiAutoMemoryType memoryType = null;

        for (String line : frontmatter.split("\n")) {
            int separator = line.indexOf(':');

            if (separator < 0) {
                continue;
            }

            String key = line.substring(0, separator)
                .trim();
            String value = line.substring(separator + 1)
                .trim();

            switch (key) {
                case "title" -> title = value;
                case "description" -> description = value;
                case "type" -> memoryType = parseType(value);
                default -> {
                    // "name" is authoritative from the path, not the frontmatter; ignore other keys.
                }
            }
        }

        return new Parsed(title, description, memoryType, content);
    }

    @Nullable
    private static AiAutoMemoryType parseType(String value) {
        try {
            return AiAutoMemoryType.valueOf(value.trim()
                .toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test --tests "*AutoMemoryFrontmatterTest" -q`
Expected: PASS (4 tests).

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/memory/AutoMemoryFrontmatter.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/memory/AutoMemoryFrontmatterTest.java
git commit -m "0_732 Add AutoMemoryFrontmatter render/parse codec"
```

### Task 3.4: `DbMemoryResource` (WritableResource backed by AiAutoMemoryService)

**Files:**
- Create: `.../memory/DbMemoryResource.java`

- [ ] **Step 1: Write the implementation** (no separate unit test — it is exercised through `DbMemoryResourceResolver` in Task 3.6)

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.aihub.tool.memory;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.ai.auto.memory.DuplicateAiAutoMemoryNameException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.WritableResource;

/**
 * A {@link WritableResource} bound to a single memory entry, identified by its slug {@code name} within a
 * {@code (workspaceId, userId, environment)} tenant. Reads render the row as a frontmatter document; writes parse the
 * document and create or update the row through {@link AiAutoMemoryService}. The backing store
 * ({@code ai_auto_memory}) remains the source of truth.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class DbMemoryResource extends AbstractResource implements WritableResource {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final long workspaceId;
    private final long userId;
    private final int environment;
    private final String name;

    DbMemoryResource(
        AiAutoMemoryService aiAutoMemoryService, long workspaceId, long userId, int environment, String name) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.environment = environment;
        this.name = name;
    }

    @Override
    public String getDescription() {
        return "DbMemoryResource[" + name + "]";
    }

    @Override
    public boolean exists() {
        return aiAutoMemoryService.read(workspaceId, userId, environment, name)
            .isPresent();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        AiAutoMemory memory = aiAutoMemoryService.read(workspaceId, userId, environment, name)
            .orElseThrow(() -> new IOException("Memory not found: " + name));

        String rendered = AutoMemoryFrontmatter.render(
            memory.getName(), memory.getTitle(), memory.getDescription(), memory.getMemoryType(), memory.getContent());

        return new ByteArrayInputStream(rendered.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public OutputStream getOutputStream() {
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();

                persist(toString(StandardCharsets.UTF_8));
            }
        };
    }

    private void persist(String text) throws IOException {
        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse(text);

        AiAutoMemoryType memoryType = parsed.memoryType() != null ? parsed.memoryType() : AiAutoMemoryType.PROJECT;
        String title = parsed.title() != null && !parsed.title()
            .isBlank() ? parsed.title() : name;

        Optional<AiAutoMemory> existing = aiAutoMemoryService.read(workspaceId, userId, environment, name);

        try {
            if (existing.isPresent()) {
                aiAutoMemoryService.update(
                    workspaceId, userId, environment, name, title, parsed.description(), memoryType, parsed.content());
            } else {
                aiAutoMemoryService.create(
                    workspaceId, userId, environment, name, title, parsed.description(), memoryType, parsed.content());
            }
        } catch (DuplicateAiAutoMemoryNameException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/memory/DbMemoryResource.java
git commit -m "0_732 Add DbMemoryResource (WritableResource over AiAutoMemoryService)"
```

### Task 3.5: `DbAutoMemoryDirectoryOps` + `DbMemoryResourceResolver` — failing test

**Files:**
- Create: `.../memory/DbAutoMemorySeamTest.java`

- [ ] **Step 1: Write the test** (covers both the resolver and directory ops over a mocked service)

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.aihub.tool.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.aihub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

class DbAutoMemorySeamTest {

    private final AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);

    private ToolContext toolContext() {
        AiHubToolInvocationContext context = new AiHubToolInvocationContext(1L, 2L, (short) 0, "prompt", 0L, "thread");

        return new ToolContext(context.toToolContext());
    }

    @Test
    void testDirectoryOpsExistsDelegatesToRead() {
        when(aiAutoMemoryService.read(1L, 2L, 0, "note"))
            .thenReturn(Optional.of(mock(AiAutoMemory.class)));

        DbAutoMemoryDirectoryOps directoryOps = new DbAutoMemoryDirectoryOps(aiAutoMemoryService);

        assertThat(directoryOps.exists("note.md", toolContext())).isTrue();
    }

    @Test
    void testDirectoryOpsDeleteStripsMdExtension() {
        DbAutoMemoryDirectoryOps directoryOps = new DbAutoMemoryDirectoryOps(aiAutoMemoryService);

        directoryOps.delete("note.md", toolContext());

        verify(aiAutoMemoryService).delete(1L, 2L, 0, "note");
    }

    @Test
    void testDirectoryOpsRenameStripsExtensions() {
        DbAutoMemoryDirectoryOps directoryOps = new DbAutoMemoryDirectoryOps(aiAutoMemoryService);

        directoryOps.rename("old.md", "new.md", toolContext());

        verify(aiAutoMemoryService).rename(1L, 2L, 0, "old", "new");
    }

    @Test
    void testDirectoryOpsListRendersIndex() {
        AiAutoMemory memory = mock(AiAutoMemory.class);

        when(memory.getName()).thenReturn("user_profile");
        when(memory.getTitle()).thenReturn("User Profile");
        when(memory.getMemoryType()).thenReturn(AiAutoMemoryType.USER);
        when(memory.getDescription()).thenReturn("who the user is");
        when(aiAutoMemoryService.listByUserAndWorkspace(1L, 2L, 0))
            .thenReturn(List.of(memory));

        DbAutoMemoryDirectoryOps directoryOps = new DbAutoMemoryDirectoryOps(aiAutoMemoryService);

        String index = directoryOps.list("MEMORY.md", toolContext());

        assertThat(index).contains("user_profile.md");
        assertThat(index).contains("User Profile");
        assertThat(index).contains("who the user is");
    }

    @Test
    void testResolverWritesCreateThroughService() throws Exception {
        when(aiAutoMemoryService.read(1L, 2L, 0, "user_profile"))
            .thenReturn(Optional.empty());

        DbMemoryResourceResolver resolver = new DbMemoryResourceResolver(aiAutoMemoryService);

        String text = AutoMemoryFrontmatter.render(
            "user_profile", "User Profile", "who", AiAutoMemoryType.USER, "body");

        try (var outputStream = resolver.resolve("user_profile.md", toolContext())
            .getOutputStream()) {

            outputStream.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        verify(aiAutoMemoryService).create(
            eq(1L), eq(2L), eq(0), eq("user_profile"), eq("User Profile"), eq("who"), eq(AiAutoMemoryType.USER),
            eq("body"));
    }

    @Test
    void testResolverThrowsWhenContextMissing() {
        DbMemoryResourceResolver resolver = new DbMemoryResourceResolver(aiAutoMemoryService);

        org.assertj.core.api.Assertions
            .assertThatThrownBy(() -> resolver.resolve("note.md", new ToolContext(Map.of())))
            .isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test --tests "*DbAutoMemorySeamTest" -q`
Expected: FAIL — `DbAutoMemoryDirectoryOps` / `DbMemoryResourceResolver` not found.

### Task 3.6: Implement the DB seams

**Files:**
- Create: `.../memory/DbAutoMemoryDirectoryOps.java`
- Create: `.../memory/DbMemoryResourceResolver.java`

- [ ] **Step 1: Write `DbMemoryResourceResolver`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.aihub.tool.memory;

import com.bytechef.ee.platform.aihub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.ai.agent.memory.MemoryResourceResolver;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.io.WritableResource;

/**
 * {@link MemoryResourceResolver} backed by {@link AiAutoMemoryService}. Resolves the requesting tenant
 * {@code (workspaceId, userId, environment)} from the Spring AI {@link ToolContext} and returns a
 * {@link DbMemoryResource} bound to the named entry.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DbMemoryResourceResolver implements MemoryResourceResolver {

    private final AiAutoMemoryService aiAutoMemoryService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DbMemoryResourceResolver(AiAutoMemoryService aiAutoMemoryService) {
        this.aiAutoMemoryService = aiAutoMemoryService;
    }

    @Override
    public WritableResource resolve(String relativePath, ToolContext toolContext) {
        AiHubToolInvocationContext context = AutoMemoryToolSupport.resolveContext(toolContext);

        String contextError = AutoMemoryToolSupport.contextError(context);

        if (contextError != null) {
            throw new IllegalStateException(contextError);
        }

        return new DbMemoryResource(
            aiAutoMemoryService, context.workspaceId(), context.userId(),
            AiHubToolInvocationContext.resolveEnvironmentOrDefault(context),
            AutoMemoryToolSupport.toMemoryName(relativePath));
    }
}
```

- [ ] **Step 2: Write `DbAutoMemoryDirectoryOps`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.aihub.tool.memory;

import com.bytechef.ee.platform.aihub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.ai.agent.memory.AutoMemoryDirectoryOps;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;

/**
 * {@link AutoMemoryDirectoryOps} backed by {@link AiAutoMemoryService}. The "index" (MEMORY.md) is synthesized from
 * {@link AiAutoMemoryService#listByUserAndWorkspace} rather than stored — the DB is the source of truth, so there is no
 * standalone index file to maintain.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DbAutoMemoryDirectoryOps implements AutoMemoryDirectoryOps {

    private final AiAutoMemoryService aiAutoMemoryService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DbAutoMemoryDirectoryOps(AiAutoMemoryService aiAutoMemoryService) {
        this.aiAutoMemoryService = aiAutoMemoryService;
    }

    @Override
    public String list(String path, ToolContext toolContext) {
        AiHubToolInvocationContext context = resolve(toolContext);

        List<AiAutoMemory> memories = aiAutoMemoryService.listByUserAndWorkspace(
            context.workspaceId(), context.userId(), AiHubToolInvocationContext.resolveEnvironmentOrDefault(context));

        if (memories.isEmpty()) {
            return "MEMORY index is empty. Create entries with MemoryCreate.";
        }

        StringBuilder stringBuilder = new StringBuilder("MEMORY index (");

        stringBuilder.append(memories.size())
            .append(" entries):\n");

        for (AiAutoMemory memory : memories) {
            stringBuilder.append("- ")
                .append(memory.getName())
                .append(".md — [")
                .append(memory.getMemoryType()
                    .name())
                .append("] ")
                .append(memory.getTitle());

            String description = memory.getDescription();

            if (description != null && !description.isBlank()) {
                stringBuilder.append(" — ")
                    .append(description);
            }

            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }

    @Override
    public boolean exists(String relativePath, ToolContext toolContext) {
        AiHubToolInvocationContext context = resolve(toolContext);

        return aiAutoMemoryService.read(
            context.workspaceId(), context.userId(),
            AiHubToolInvocationContext.resolveEnvironmentOrDefault(context),
            AutoMemoryToolSupport.toMemoryName(relativePath))
            .isPresent();
    }

    @Override
    public void delete(String relativePath, ToolContext toolContext) {
        AiHubToolInvocationContext context = resolve(toolContext);

        aiAutoMemoryService.delete(
            context.workspaceId(), context.userId(),
            AiHubToolInvocationContext.resolveEnvironmentOrDefault(context),
            AutoMemoryToolSupport.toMemoryName(relativePath));
    }

    @Override
    public void rename(String oldRelativePath, String newRelativePath, ToolContext toolContext) {
        AiHubToolInvocationContext context = resolve(toolContext);

        aiAutoMemoryService.rename(
            context.workspaceId(), context.userId(),
            AiHubToolInvocationContext.resolveEnvironmentOrDefault(context),
            AutoMemoryToolSupport.toMemoryName(oldRelativePath), AutoMemoryToolSupport.toMemoryName(newRelativePath));
    }

    private static AiHubToolInvocationContext resolve(ToolContext toolContext) {
        AiHubToolInvocationContext context = AutoMemoryToolSupport.resolveContext(toolContext);

        String contextError = AutoMemoryToolSupport.contextError(context);

        if (contextError != null) {
            throw new IllegalStateException(contextError);
        }

        return context;
    }
}
```

- [ ] **Step 3: Add `toMemoryName` helper to `AutoMemoryToolSupport`** (strip a trailing `.md` and lowercase; the `name` slug is authoritative)

In `AutoMemoryToolSupport.java`, add this method (keep the class `final`, package-private):
```java
    /**
     * Derives the memory {@code name} slug from a tool-supplied path: strips an optional {@code .md} suffix and lower-
     * cases. The slug — not the path — is the authoritative key for {@link com.bytechef.platform.ai.auto.memory.AiAutoMemory}.
     */
    static String toMemoryName(String path) {
        String trimmed = path == null ? "" : path.trim();

        if (trimmed.endsWith(".md")) {
            trimmed = trimmed.substring(0, trimmed.length() - ".md".length());
        }

        return trimmed.toLowerCase();
    }
```

- [ ] **Step 4: Run the seam test to verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test --tests "*DbAutoMemorySeamTest" -q`
Expected: PASS (6 tests).

- [ ] **Step 5: Format + commit**

```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:spotlessApply -q
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/memory/DbMemoryResourceResolver.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/memory/DbAutoMemoryDirectoryOps.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/memory/AutoMemoryToolSupport.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/memory/DbAutoMemorySeamTest.java
git commit -m "0_732 Add DB-backed MemoryResourceResolver + AutoMemoryDirectoryOps"
```

---

## Phase 4 — Wire the advisor; remove the six callbacks

### Task 4.1: Delete the six callbacks and their tests

**Files (delete):**
- `.../memory/ReadAutoMemoryToolCallback.java`
- `.../memory/CreateAutoMemoryToolCallback.java`
- `.../memory/UpdateAutoMemoryToolCallback.java`
- `.../memory/DeleteAutoMemoryToolCallback.java`
- `.../memory/RenameAutoMemoryToolCallback.java`
- `.../memory/ListAutoMemoriesToolCallback.java`
- `.../memory/CreateAutoMemoryToolCallbackTest.java`
- `.../memory/UpdateAutoMemoryToolCallbackTest.java`
- `.../memory/DeleteAutoMemoryToolCallbackTest.java`
- `.../memory/RenameAutoMemoryToolCallbackTest.java`
- (plus any `ReadAutoMemoryToolCallbackTest.java` / `ListAutoMemoriesToolCallbackTest.java` if present)

- [ ] **Step 1: Delete them**

```bash
cd /Volumes/Data/bytechef/bytechef
d=server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src
git rm "$d/main/java/com/bytechef/ee/platform/aihub/tool/memory/ReadAutoMemoryToolCallback.java" \
       "$d/main/java/com/bytechef/ee/platform/aihub/tool/memory/CreateAutoMemoryToolCallback.java" \
       "$d/main/java/com/bytechef/ee/platform/aihub/tool/memory/UpdateAutoMemoryToolCallback.java" \
       "$d/main/java/com/bytechef/ee/platform/aihub/tool/memory/DeleteAutoMemoryToolCallback.java" \
       "$d/main/java/com/bytechef/ee/platform/aihub/tool/memory/RenameAutoMemoryToolCallback.java" \
       "$d/main/java/com/bytechef/ee/platform/aihub/tool/memory/ListAutoMemoriesToolCallback.java"
git rm $(git ls-files "$d/test/java/com/bytechef/ee/platform/aihub/tool/memory/*ToolCallbackTest.java")
```

- [ ] **Step 2: Do NOT build yet** — `AiHubConfiguration` still references `registerAutoMemoryToolCallbacks`, fixed in Task 4.3. Commit the deletion together with the wiring change. Proceed.

### Task 4.2: Trim `AutoMemoryToolSupport` (drop artifact-recording helpers)

**Files:**
- Modify: `.../memory/AutoMemoryToolSupport.java`

- [ ] **Step 1: Remove the now-unused artifact-recording members**

Delete from `AutoMemoryToolSupport.java`:
- the `recordArtifact(...)` method (the `static boolean recordArtifact(...)` block),
- the `recordMissingThreadId(...)` method,
- and these now-unused imports: `com.bytechef.ee.platform.aihub.task.AiHubTaskArtifactKind`, `com.bytechef.ee.platform.aihub.task.AiHubTaskArtifactService`, `io.micrometer.core.instrument.Metrics`, `java.util.Map`, `java.util.concurrent.atomic.AtomicBoolean`, `org.slf4j.Logger`.

Keep: `resolveContext`, `toolError`, `contextError`, `parseMemoryType`, `formatTimestamp`, and the new `toMemoryName`. Keep imports actually used by those (`AiHubToolInvocationContext`, `ToolErrors`, `AiAutoMemory`, `AiAutoMemoryType`, `LocalDateTime`, `DateTimeFormatter`, `Nullable`, `ToolContext`, `JsonMapper`).

- [ ] **Step 2: Proceed to Task 4.3 before building** (compilation depends on the config change).

### Task 4.3: Wire the advisor into the BUILD agent

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java`

- [ ] **Step 1: Add the fork dependency** to `automation-ai-hub-service/build.gradle.kts` (near the existing tool-search vendored-fork deps, ~line 6):

```kotlin
    implementation(project(":spring-ai-agent-utils:auto-memory"))
```

- [ ] **Step 2: In `AiHubConfiguration.java`, replace the registration call.**

Find (around line 482):
```java
        registerAutoMemoryToolCallbacks(toolCallbacks, aiHubMemoryService, taskArtifactService);
```
Replace with:
```java
        // Auto-memory is now exposed via the forked AutoMemoryToolsAdvisor (DB-backed Resource seam),
        // registered as an advisor below rather than as standalone tool callbacks.
```

Find (around line 493):
```java
            .memoryIndexResolver(buildMemoryIndexResolver(aiHubMemoryService))
```
Delete that line (the index is now the virtual MEMORY.md served by the advisor's MemoryView).

Find the `toolSearchToolCallAdvisorProvider.ifAvailable(buildBuilder::advisor);` line (around line 506) and add, immediately after it:
```java
            buildBuilder.advisor(
                AutoMemoryToolsAdvisor.builder()
                    .autoMemoryTools(
                        new AutoMemoryTools(
                            new DbMemoryResourceResolver(aiHubMemoryService),
                            new DbAutoMemoryDirectoryOps(aiHubMemoryService)))
                    .memorySystemPrompt(promptAiHubAutoMemoryToolsResource)
                    .build());
```

- [ ] **Step 3: Add the imports** to `AiHubConfiguration.java`:
```java
import com.bytechef.ee.platform.aihub.tool.memory.DbAutoMemoryDirectoryOps;
import com.bytechef.ee.platform.aihub.tool.memory.DbMemoryResourceResolver;
import com.bytechef.platform.ai.agent.memory.AutoMemoryTools;
import com.bytechef.platform.ai.agent.memory.AutoMemoryToolsAdvisor;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
```
(Skip any import already present.)

- [ ] **Step 4: Inject the ByteChef prompt resource.** Add a field + constructor/`@Value` parameter alongside the existing `promptAiHubBuildResource` injection. Locate how `promptAiHubBuildResource` is declared (it is passed to `getSystemPrompt(...)`). Mirror that mechanism. If prompts are injected via `@Value` on the bean method, add a parameter to the BUILD-agent bean method:
```java
        @Value("classpath:prompt/ai_hub_auto_memory_tools_system_prompt.md") Resource promptAiHubAutoMemoryToolsResource,
```
(If `promptAiHubBuildResource` is a field, declare `promptAiHubAutoMemoryToolsResource` the same way and the same place.)

- [ ] **Step 5: Delete the now-unused private helpers** `registerAutoMemoryToolCallbacks(...)` and `buildMemoryIndexResolver(...)` from `AiHubConfiguration.java`. Remove the now-unused imports they required (`AiAutoMemory` import if only used there; keep `AiAutoMemoryService` — still used to build the seams). If `taskArtifactService` is now unused in this bean method, leave its other usages intact (it is also passed to `CreateAssetFileToolCallback` via `aiHubTaskArtifactRecorder` — verify before removing any parameter).

- [ ] **Step 6: Handle `AiHubSpringAIAgent.MemoryIndexResolver`.** Search for other usages:

Run: `grep -rn "memoryIndexResolver\|MemoryIndexResolver" server/ee --include=*.java | grep -v AiHubConfiguration`
- If the only remaining references are the interface declaration + field + builder method in `AiHubSpringAIAgent.java` and its own internal use, leave the interface in place (removing it is optional and out of scope). If it is used to inject the index into the system prompt at runtime, **leave `AiHubSpringAIAgent.java` untouched** — passing no resolver simply means no injected index, which is the intended new behavior (the advisor's MemoryView serves the index instead). Do not delete the interface in this task to keep the change focused.

### Task 4.4: ByteChef memory system prompt

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt/ai_hub_auto_memory_tools_system_prompt.md`

- [ ] **Step 1: Write the prompt** (no unescaped `{`/`}` — `PromptTemplate` would treat them as variables)

```markdown
## Long-term memory

You have a persistent, per-user long-term memory accessed through the Memory tools:
MemoryView, MemoryCreate, MemoryStrReplace, MemoryInsert, MemoryDelete, MemoryRename.

- Start by calling MemoryView on "MEMORY.md" to see the index of what you already remember.
- Each memory entry is a file named "<slug>.md" whose body begins with a frontmatter block:

  ---
  name: <slug>
  title: <human-readable title>
  description: <one-line summary shown in the index>
  type: USER
  ---
  <the memory body>

- type is one of USER (profile / preferences), FEEDBACK (corrections / confirmed approaches),
  PROJECT (decisions / deadlines), or REFERENCE (external pointers).
- Use MemoryCreate when the user shares a durable fact; use MemoryStrReplace / MemoryInsert to
  update an existing entry; use MemoryDelete when an entry is stale or wrong.
- The index (MEMORY.md) is maintained automatically — never create or edit it by hand.
- Do not store ephemeral conversation state, secrets, or anything already in the codebase.
```

- [ ] **Step 2: Build the two EE modules**

Run:
```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:compileJava \
          :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava -q
```
Expected: BUILD SUCCESSFUL (no references to deleted callbacks remain).

- [ ] **Step 3: Run the affected module test suites**

Run:
```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test \
          :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test -q
```
Expected: PASS. (The four deleted `*ToolCallbackTest` classes are gone; remaining tests compile and pass.)

- [ ] **Step 4: Format + commit (deletion + wiring together)**

```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:spotlessApply \
          :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply -q
git add -A server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service \
            server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service
git commit -m "0_732 Expose forked AutoMemoryToolsAdvisor on BUILD agent; remove bespoke memory callbacks"
```

---

## Phase 5 — Remove the reverser vaporware

This phase is independent of Phases 1–4 and independently committable.

### Task 5.1: Drop the `reversible` flag from `AiHubTaskArtifactKind`

**Files:**
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/task/AiHubTaskArtifactKind.java`

- [ ] **Step 1: Remove the flag and the `ArtifactReverser` Javadoc.** Replace the whole file body with the version below (values and ordinals unchanged — only the `reversible` flag, constructor, accessor, and reverser Javadoc are gone):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.task;

/**
 * Classifies the type of side-effect artifact that was produced during a AI Hub task turn.
 *
 * <p>
 * <b>Append-only.</b> The values are persisted as INT ordinals via Spring Data JDBC and
 * {@link com.bytechef.ee.platform.aihub.util.EnumOrdinals} — reordering or deleting a value would silently re-map every
 * historical row to the wrong kind. New values MUST be appended at the end. The
 * {@code EnumOrdinalStabilityTest#testTaskArtifactKindOrdinals} pinning test enforces this at build time.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubTaskArtifactKind {

    // append-only
    FILE_CREATED,
    BINARY_FILE_CREATED,
    WORKFLOW_CREATED,
    WORKFLOW_UPDATED,
    DATA_TABLE_ROW_ADDED,
    DATA_TABLE_ROW_UPDATED,
    DATA_TABLE_ROW_DELETED,
    DATA_TABLE_COLUMN_ADDED,
    KB_DOCUMENT_ADDED,
    KB_DOCUMENT_DELETED,
    WORKFLOW_EXECUTION_STARTED,
    MEMORY_CREATED,
    MEMORY_UPDATED,
    MEMORY_DELETED,
    MEMORY_RENAMED,

    // User-attached references via the composer plus-button menu — no side effect to undo on the underlying
    // entity. Appended at the END of the enum to preserve ordinal stability per the JDBC enum-storage
    // convention (new values get the next ordinals; existing rows keep their ordinals stable).
    FILE_REFERENCED,
    WORKFLOW_REFERENCED,
    DATA_TABLE_REFERENCED,
    KB_REFERENCED,

    // Agent-template referenced resources — the four composer resource kinds (MCP server, API collection,
    // workflow execution, task) copied onto a spawned task from a personal-agent template. Appended at the
    // END per the JDBC enum-storage convention so the ordinals of all earlier values stay pinned.
    MCP_SERVER_REFERENCED,
    API_COLLECTION_REFERENCED,
    WORKFLOW_EXECUTION_REFERENCED,
    TASK_REFERENCED
}
```

- [ ] **Step 2: Confirm nothing reads `.reversible()`**

Run: `grep -rn "\.reversible()" server/`
Expected: no matches.

### Task 5.2: Remove `REVERSED` from `AiHubTaskArtifactStatus`

**Files:**
- Modify: `.../task/AiHubTaskArtifactStatus.java`

- [ ] **Step 1: Remove the value and scrub the reverser Javadoc.** Replace the file with:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.task;

/**
 * Lifecycle status of a {@link AiHubTaskArtifact}.
 *
 * <ul>
 * <li>{@code APPLIED} — the side-effect was applied.</li>
 * <li>{@code EXPIRED} — the artifact's retention window has elapsed.</li>
 * <li>{@code IRREVERSIBLE} — the kind of side-effect cannot be reversed (e.g. workflow execution).</li>
 * </ul>
 *
 * <p>
 * <strong>Ordinal stability is load-bearing.</strong> The status column is persisted as an INT ordinal (see the
 * Liquibase migration); reordering, renaming, or removing values silently re-attributes historical rows. New values
 * MUST be appended below the {@code // append-only} marker and never inserted between existing entries.
 * {@code EnumOrdinalStabilityTest} pins the current order — it will fail the build before such a change reaches
 * production.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubTaskArtifactStatus {

    APPLIED,
    EXPIRED,
    IRREVERSIBLE;
    // append-only — add new values BELOW this line. Reordering breaks ordinal-based persistence.
}
```

> `REVERSED` was ordinal 3 (the last value) and is never assigned anywhere, so removing it does not shift any
> other ordinal or re-attribute any persisted row.

- [ ] **Step 2: Confirm `REVERSED` is unreferenced on the server**

Run: `grep -rn "REVERSED\|AiHubTaskArtifactStatus.REVERSED" server/ --include=*.java`
Expected: no matches (after this edit). If any remain, they must be removed.

### Task 5.3: Update `EnumOrdinalStabilityTest`

**Files:**
- Modify: `.../util/EnumOrdinalStabilityTest.java`

- [ ] **Step 1: Drop the REVERSED assertion** in `testTaskArtifactStatusOrdinalsAreStable`. Remove the line:
```java
        expected.put("REVERSED", 3);
```
Leave `APPLIED`/`EXPIRED`/`IRREVERSIBLE` (ordinals 0–2). Leave `testTaskArtifactKindOrdinalsAreStable` unchanged (all kind ordinals, including `MEMORY_*` at 11–14, are preserved).

- [ ] **Step 2: Run the api module tests**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:test -q`
Expected: PASS (ordinal pinning + wire-format tests green; `AiHubTaskArtifactKindWireFormatTest` still lists all 23 kinds, which are unchanged).

- [ ] **Step 3: Format + commit**

```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:spotlessApply -q
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/task/AiHubTaskArtifactKind.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/task/AiHubTaskArtifactStatus.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/util/EnumOrdinalStabilityTest.java
git commit -m "0_732 Remove dead reverser vaporware (reversible flag, REVERSED status)"
```

### Task 5.4: Remove `REVERSED` from the GraphQL schema

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-artifact.graphqls`

- [ ] **Step 1: Remove the enum value.** In the `enum AiHubTaskArtifactStatus { ... }` block (around lines 41–46), delete the `REVERSED` line so it reads:

```graphql
enum AiHubTaskArtifactStatus {
    APPLIED
    EXPIRED
    IRREVERSIBLE
}
```

- [ ] **Step 2: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-artifact.graphqls
git commit -m "0_732 client - Remove REVERSED from AiHubTaskArtifactStatus GraphQL enum"
```

### Task 5.5: Remove `REVERSED` from the client

**Files:**
- Modify: `client/src/pages/automation/ai-hub/tasks/api/tasks.api.ts:58`
- Modify: `client/src/pages/automation/ai-hub/AiHubArtifactHistoryPage.tsx`
- Regenerate: `client/src/shared/middleware/graphql-types.ts`

- [ ] **Step 1: Edit the manual union type** in `tasks.api.ts` (line ~58):

From:
```ts
export type AiHubArtifactStatusType = 'APPLIED' | 'EXPIRED' | 'IRREVERSIBLE' | 'REVERSED';
```
To:
```ts
export type AiHubArtifactStatusType = 'APPLIED' | 'EXPIRED' | 'IRREVERSIBLE';
```

- [ ] **Step 2: Remove the display-map entry** in `AiHubArtifactHistoryPage.tsx`. Find the status→label map containing `REVERSED: 'Reversed'` and delete that entry. Verify no other code branches on `'REVERSED'` in that file:

Run: `grep -n "REVERSED\|Reversed" client/src/pages/automation/ai-hub/AiHubArtifactHistoryPage.tsx`
Expected after edit: no matches.

- [ ] **Step 3: Regenerate GraphQL types** (the schema changed in Task 5.4):

Run:
```bash
cd client && npx graphql-codegen && cd ..
```
Expected: `client/src/shared/middleware/graphql-types.ts` regenerated; `Reversed = 'REVERSED'` removed from the `AiHubTaskArtifactStatus` enum there.

- [ ] **Step 4: Verify no stray references remain**

Run: `grep -rn "REVERSED\|Reversed" client/src`
Expected: no matches.

- [ ] **Step 5: Client checks**

Run: `cd client && npm run check && cd ..`
Expected: lint + typecheck + tests PASS.

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/automation/ai-hub/tasks/api/tasks.api.ts \
        client/src/pages/automation/ai-hub/AiHubArtifactHistoryPage.tsx \
        client/src/shared/middleware/graphql-types.ts
git commit -m "0_732 client - Remove REVERSED artifact status from client"
```

---

## Phase 6 — Full verification

### Task 6.1: Build + check the touched server modules

- [ ] **Step 1: Compile everything affected**

Run:
```bash
./gradlew :spring-ai-agent-utils:auto-memory:check \
          :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:check \
          :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:check \
          :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check -q
```
Expected: BUILD SUCCESSFUL (spotless, checkstyle, PMD, spotbugs, tests all green).

- [ ] **Step 2: Confirm the fork uses no `java.nio.file`**

Run: `grep -rn "java.nio.file" spring-ai-agent-utils/auto-memory/src/main`
Expected: no matches (the fork is filesystem-free).

- [ ] **Step 3: Confirm no dangling references to deleted symbols**

Run:
```bash
grep -rn "AutoMemoryToolCallback\|registerAutoMemoryToolCallbacks\|buildMemoryIndexResolver\|ArtifactReverser\|AiHubTaskArtifactReversalService" server/ --include=*.java
```
Expected: no matches.

- [ ] **Step 4: Client final check**

Run: `cd client && npm run check && cd ..`
Expected: PASS.

---

## Self-Review notes (addressed)

- **Spec coverage:** fork module (Phase 0–2) ↔ spec §"Module layout"/"Fork changes"; DB seams (Phase 3) ↔ spec §"ByteChef EE side"; advisor wiring + callback removal + virtual MEMORY.md (Phase 4) ↔ spec §"ByteChef EE wiring"/D9; reverser cleanup (Phase 5) ↔ spec §"Removed"/D8. Per-tenant scoping (D5) implemented via `ToolContext` in the DB seams. Ordinal-stability (D7) preserved — `MEMORY_*` kinds untouched.
- **Repackaging decision** (not in the original spec) added in Phase 0/1 after discovering upstream `spring-ai-agent-utils` stays on the classpath for `AskUserQuestionTool` et al.
- **Type consistency:** `MemoryResourceResolver.resolve(String, ToolContext) -> WritableResource`, `AutoMemoryDirectoryOps.{list,exists,delete,rename}`, `AutoMemoryToolsAdvisor.builder().autoMemoryTools(...).memorySystemPrompt(Resource)`, `AutoMemoryFrontmatter.{render,parse}` / `Parsed(title,description,memoryType,content)`, `AutoMemoryToolSupport.toMemoryName(String)` — names used consistently across tasks.
- **Known follow-up (out of scope):** `AiHubSpringAIAgent.MemoryIndexResolver` interface is left in place (Task 4.3 Step 6); removing it is optional and deferred to keep the change focused.
