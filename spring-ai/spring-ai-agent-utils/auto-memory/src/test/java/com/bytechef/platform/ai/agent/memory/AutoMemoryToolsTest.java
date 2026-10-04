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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.WritableResource;

class AutoMemoryToolsTest {

    private final Map<String, String> store = new LinkedHashMap<>();
    private AutoMemoryTools autoMemoryTools;

    @BeforeEach
    void setUp() {
        store.clear();

        MemoryResourceResolver resolver = (relativePath) -> new FakeMemoryResource(relativePath);

        AutoMemoryDirectoryOps directoryOps = new AutoMemoryDirectoryOps() {
            @Override
            public String list(String path) {
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
            public boolean exists(String relativePath) {
                return store.containsKey(relativePath);
            }

            @Override
            public void delete(String relativePath) {
                store.remove(relativePath);
            }

            @Override
            public void rename(String oldRelativePath, String newRelativePath) {
                store.put(newRelativePath, store.remove(oldRelativePath));
            }
        };

        autoMemoryTools = new AutoMemoryTools(resolver, directoryOps);
    }

    @Test
    void testMemoryCreateThenViewRoundTrips() {
        String createResult = autoMemoryTools.memoryCreate("user_profile.md", "hello world");

        assertThat(createResult).contains("Successfully created");
        assertThat(store).containsKey("user_profile.md");

        String viewResult = autoMemoryTools.memoryView("user_profile.md", null);

        assertThat(viewResult).contains("hello world");
    }

    @Test
    void testMemoryCreateRejectsExistingFile() {
        store.put("user_profile.md", "existing");

        String result = autoMemoryTools.memoryCreate("user_profile.md", "new");

        assertThat(result).contains("already exists");
    }

    @Test
    void testMemoryViewClampsAnEndPastTheLastLine() {
        store.put("notes.md", "one\ntwo\nthree");

        String result = autoMemoryTools.memoryView("notes.md", "2,100");

        assertThat(result).contains("Lines 2-3 of 3")
            .contains("two")
            .doesNotContain("one");
    }

    @Test
    void testMemoryViewRejectsAStartPastTheLastLine() {
        store.put("notes.md", "one\ntwo\nthree");

        assertThat(autoMemoryTools.memoryView("notes.md", "10,20"))
            .isEqualTo("Error: viewRange starts at line 10, past the end of the entry (3 lines)");
    }

    @Test
    void testMemoryViewRejectsAStartAfterTheEnd() {
        store.put("notes.md", "one\ntwo\nthree");

        assertThat(autoMemoryTools.memoryView("notes.md", "3,1"))
            .isEqualTo("Error: viewRange start 3 is after its end 1");
    }

    @Test
    void testMemoryViewOnRootListsIndex() {
        store.put("user_profile.md", "x");

        String result = autoMemoryTools.memoryView("", null);

        assertThat(result).contains("MEMORY index");
        assertThat(result).contains("user_profile.md");
    }

    @Test
    void testEverySpellingOfTheIndexPathIsTreatedAsTheIndex() {
        store.put("user_profile.md", "x");

        for (String indexPath : new String[] {
            "/", "//", "MEMORY.md", "/MEMORY.md", " memory.md ", "Memory.MD"
        }) {
            assertThat(autoMemoryTools.memoryView(indexPath, null)).contains("MEMORY index");
            assertThat(autoMemoryTools.memoryCreate(indexPath, "text")).startsWith("Error: The index is maintained");
            assertThat(autoMemoryTools.memoryStrReplace(indexPath, "x", "y"))
                .startsWith("Error: The index is maintained");
            assertThat(autoMemoryTools.memoryInsert(indexPath, 0, "y")).startsWith("Error: The index is maintained");
            assertThat(autoMemoryTools.memoryDelete(indexPath)).isEqualTo("Error: The index cannot be deleted.");
            assertThat(autoMemoryTools.memoryRename("user_profile.md", indexPath))
                .isEqualTo("Error: The index cannot be renamed.");
        }

        assertThat(store).containsOnlyKeys("user_profile.md");
    }

    @Test
    void testMemoryStrReplaceRejectsAnEmptyOldStr() {
        store.put("note.md", "the quick brown fox");

        assertThat(autoMemoryTools.memoryStrReplace("note.md", "", "red"))
            .isEqualTo("Error: oldStr must not be empty.");
        assertThat(autoMemoryTools.memoryStrReplace("note.md", null, "red"))
            .isEqualTo("Error: oldStr must not be empty.");
        assertThat(store.get("note.md")).isEqualTo("the quick brown fox");
    }

    @Test
    void testMemoryStrReplaceEditsContent() {
        store.put("note.md", "the quick brown fox");

        String result = autoMemoryTools.memoryStrReplace("note.md", "brown", "red");

        assertThat(result).contains("Successfully edited");
        assertThat(store.get("note.md")).isEqualTo("the quick red fox");
    }

    @Test
    void testMemoryStrReplaceRejectsNonUniqueMatch() {
        store.put("note.md", "ab ab");

        String result = autoMemoryTools.memoryStrReplace("note.md", "ab", "x");

        assertThat(result).contains("appears 2 times");
        assertThat(store.get("note.md")).isEqualTo("ab ab");
    }

    @Test
    void testMemoryInsertAddsLine() {
        store.put("note.md", "line1\nline2");

        String result = autoMemoryTools.memoryInsert("note.md", 1, "inserted");

        assertThat(result).contains("Successfully inserted");
        assertThat(store.get("note.md")).isEqualTo("line1\ninserted\nline2");
    }

    @Test
    void testMemoryDeleteRemovesFile() {
        store.put("note.md", "x");

        String result = autoMemoryTools.memoryDelete("note.md");

        assertThat(result).contains("Successfully deleted");
        assertThat(store).doesNotContainKey("note.md");
    }

    @Test
    void testMemoryRenameMovesFile() {
        store.put("old.md", "x");

        String result = autoMemoryTools.memoryRename("old.md", "new.md");

        assertThat(result).contains("Successfully renamed");
        assertThat(store).doesNotContainKey("old.md");
        assertThat(store).containsKey("new.md");
    }

    @Test
    void testMemoryInsertRejectsADelimiterLineBeforeTheFrontmatterCloses() {
        String entry = "---\ntitle: Notes\ntype: USER\n---\nbody\n";

        store.put("notes.md", entry);

        String expectedError =
            "Error: a '---' line cannot be inserted before line 4, which closes the frontmatter. Insert body text "
                + "after line 4.";

        assertThat(autoMemoryTools.memoryInsert("notes.md", 2, "---")).isEqualTo(expectedError);
        assertThat(autoMemoryTools.memoryInsert("notes.md", 3, "extra\n  ---  ")).isEqualTo(expectedError);
        assertThat(autoMemoryTools.memoryInsert("notes.md", 0, "---\ntitle: Other\n---")).isEqualTo(expectedError);
        assertThat(store.get("notes.md")).isEqualTo(entry);
    }

    @Test
    void testMemoryInsertAcceptsADelimiterLineInTheBody() {
        store.put("notes.md", "---\ntitle: Notes\n---\nbody\n");

        assertThat(autoMemoryTools.memoryInsert("notes.md", 4, "---")).startsWith("Successfully inserted");
        assertThat(store.get("notes.md")).isEqualTo("---\ntitle: Notes\n---\nbody\n---\n");
    }

    @Test
    void testMemoryInsertRejectsAMissingOrNegativeLine() {
        store.put("notes.md", "one\ntwo");

        assertThat(autoMemoryTools.memoryInsert("notes.md", null, "x"))
            .isEqualTo("Error: insertLine must be a non-negative integer");
        assertThat(autoMemoryTools.memoryInsert("notes.md", -1, "x"))
            .isEqualTo("Error: insertLine must be a non-negative integer");
        assertThat(store.get("notes.md")).isEqualTo("one\ntwo");
    }

    @Test
    void testMemoryInsertRejectsALinePastTheEnd() {
        store.put("notes.md", "one\ntwo");

        assertThat(autoMemoryTools.memoryInsert("notes.md", 3, "x"))
            .isEqualTo("Error: insertLine 3 exceeds file length of 2 lines");
        assertThat(store.get("notes.md")).isEqualTo("one\ntwo");
    }

    @Test
    void testMemoryInsertIntoAMissingEntryIsAnError() {
        assertThat(autoMemoryTools.memoryInsert("missing.md", 0, "x"))
            .isEqualTo("Error: File does not exist: missing.md");
        assertThat(store).isEmpty();
    }

    @Test
    void testMemoryStrReplaceReportsAnOldStrThatIsNotFound() {
        store.put("note.md", "the quick brown fox");

        assertThat(autoMemoryTools.memoryStrReplace("note.md", "purple", "red"))
            .isEqualTo("Error: oldStr not found in file: note.md");
        assertThat(store.get("note.md")).isEqualTo("the quick brown fox");
    }

    @Test
    void testMemoryViewOfAMissingEntryIsAnError() {
        assertThat(autoMemoryTools.memoryView("missing.md", null)).isEqualTo("Error: Path does not exist: missing.md");
    }

    @Test
    void testMemoryViewRejectsAMalformedRange() {
        store.put("notes.md", "one\ntwo\nthree");

        assertThat(autoMemoryTools.memoryView("notes.md", "1"))
            .isEqualTo("Error: viewRange must be 'start,end' (e.g. '1,50')");
        assertThat(autoMemoryTools.memoryView("notes.md", "abc,2"))
            .isEqualTo("Error: viewRange must be 'start,end' integers (e.g. '1,50')");
    }

    @Test
    void testMemoryRenameFromAMissingSourceIsAnError() {
        store.put("other.md", "x");

        assertThat(autoMemoryTools.memoryRename("missing.md", "new.md"))
            .isEqualTo("Error: Source path does not exist: missing.md");
        assertThat(store).containsOnlyKeys("other.md");
    }

    @Test
    void testMemoryRenameOntoAnExistingEntryIsAnError() {
        store.put("old.md", "old");
        store.put("new.md", "new");

        assertThat(autoMemoryTools.memoryRename("old.md", "new.md"))
            .isEqualTo("Error: Destination path already exists: new.md");
        assertThat(store).containsEntry("old.md", "old")
            .containsEntry("new.md", "new");
    }

    @Test
    void testInsertingAfterTheLastViewedLineAppends() {
        store.put("notes.md", "one\ntwo\n");

        assertThat(autoMemoryTools.memoryView("notes.md", null)).contains("Lines 1-2 of 2");
        assertThat(autoMemoryTools.memoryInsert("notes.md", 2, "three")).startsWith("Successfully inserted");
        assertThat(store.get("notes.md")).isEqualTo("one\ntwo\nthree\n");
    }

    @Test
    void testExpectedFailuresAreReportedWithTheirMessage() {
        AutoMemoryTools failingTools = new AutoMemoryTools(
            (relativePath) -> new FakeMemoryResource(relativePath),
            new FailingDirectoryOps(new IOException("Memory entry does not exist: gone.md")));

        assertThat(failingTools.memoryDelete("gone.md"))
            .isEqualTo("Error: Memory entry does not exist: gone.md");
    }

    @Test
    void testUnavailableMemoryIsReportedWithItsMessage() {
        AutoMemoryTools failingTools = new AutoMemoryTools(
            (relativePath) -> new FakeMemoryResource(relativePath),
            new FailingDirectoryOps(new AutoMemoryUnavailableException("Auto memory is not available")));

        assertThat(failingTools.memoryView("MEMORY.md", null)).isEqualTo("Error: Auto memory is not available");
    }

    @Test
    void testRejectedInputIsReportedWithItsMessage() {
        AutoMemoryTools failingTools = new AutoMemoryTools(
            (relativePath) -> new FakeMemoryResource(relativePath),
            new FailingDirectoryOps(new IllegalArgumentException("title is required")));

        assertThat(failingTools.memoryDelete("notes.md")).isEqualTo("Error: title is required");
    }

    @Test
    void testAnUnrelatedUnsupportedOperationIsAnUnexpectedFailure() {
        AutoMemoryTools failingTools = new AutoMemoryTools(
            (relativePath) -> new FakeMemoryResource(relativePath),
            new FailingDirectoryOps(new UnsupportedOperationException()));

        assertThat(failingTools.memoryView("MEMORY.md", null))
            .startsWith("Error: the memory operation failed and was not applied");
    }

    @Test
    void testUnexpectedFailuresHideTheirMessage() {
        AutoMemoryTools failingTools = new AutoMemoryTools(
            (relativePath) -> new FakeMemoryResource(relativePath),
            new FailingDirectoryOps(new IllegalStateException("SELECT * FROM ai_auto_memory WHERE principal_id = 42")));

        String result = failingTools.memoryView("MEMORY.md", null);

        assertThat(result).startsWith("Error: the memory operation failed and was not applied");
        assertThat(result).doesNotContain("ai_auto_memory");
    }

    private record FailingDirectoryOps(Exception exception) implements AutoMemoryDirectoryOps {

        @Override
        public String list(String path) {
            throw toUnchecked();
        }

        @Override
        public boolean exists(String relativePath) {
            return true;
        }

        @Override
        public void delete(String relativePath) throws IOException {
            throw toThrown();
        }

        @Override
        public void rename(String oldRelativePath, String newRelativePath)
            throws IOException {

            throw toThrown();
        }

        private IOException toThrown() {
            if (exception instanceof IOException ioException) {
                return ioException;
            }

            throw toUnchecked();
        }

        private RuntimeException toUnchecked() {
            if (exception instanceof RuntimeException runtimeException) {
                return runtimeException;
            }

            return new UncheckedIOException((IOException) exception);
        }
    }

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
