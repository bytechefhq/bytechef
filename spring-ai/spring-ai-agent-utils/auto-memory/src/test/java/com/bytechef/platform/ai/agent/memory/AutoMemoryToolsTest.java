/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
