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

package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.ai.agent.memory.AutoMemoryTools;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryServiceImpl;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.ai.auto.memory.repository.filestorage.FileStorageAiAutoMemoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AutoMemoryResourceTest {

    private static final AiAutoMemoryOwner OWNER = new AiAutoMemoryOwner(
        1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 100L, Environment.DEVELOPMENT);

    private static final AiAutoMemoryOwner OTHER_OWNER = new AiAutoMemoryOwner(
        1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 101L, Environment.DEVELOPMENT);

    private static final String PROFILE_ENTRY = """
        ---
        title: Profile
        description: How the user likes replies
        type: USER
        ---
        Prefers concise replies.""";

    @TempDir
    private Path tempDir;

    private AiAutoMemoryService aiAutoMemoryService;
    private AutoMemoryTools autoMemoryTools;

    @BeforeEach
    void beforeEach() {
        aiAutoMemoryService = new AiAutoMemoryServiceImpl(
            new FileStorageAiAutoMemoryRepository(new FilesystemFileStorageService(tempDir.toString())));

        autoMemoryTools = new AutoMemoryTools(
            new AutoMemoryResourceResolver(aiAutoMemoryService, OWNER),
            new ServiceBackedAutoMemoryDirectoryOps(aiAutoMemoryService, OWNER));

        assertThat(autoMemoryTools.memoryCreate("profile.md", PROFILE_ENTRY))
            .startsWith("Successfully created");
    }

    @Test
    void testCreateStoresEveryFrontmatterField() {
        AiAutoMemory memory = stored("profile");

        assertThat(memory.getTitle()).isEqualTo("Profile");
        assertThat(memory.getDescription()).isEqualTo("How the user likes replies");
        assertThat(memory.getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(memory.getContent()).isEqualTo("Prefers concise replies.");
    }

    @Test
    void testEditingTheBodyKeepsTheMetadata() {
        assertThat(autoMemoryTools.memoryStrReplace("profile.md", "concise", "short"))
            .startsWith("Successfully edited");

        AiAutoMemory memory = stored("profile");

        assertThat(memory.getContent()).isEqualTo("Prefers short replies.");
        assertThat(memory.getTitle()).isEqualTo("Profile");
        assertThat(memory.getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
    }

    @Test
    void testRemovingTheDescriptionLineClearsTheDescription() {
        assertThat(
            autoMemoryTools.memoryStrReplace("profile.md", "description: How the user likes replies\n", ""))
                .startsWith("Successfully deleted");

        assertThat(stored("profile")
            .getDescription()).isNull();
    }

    @Test
    void testRemovingTheTypeLineKeepsTheStoredType() {
        autoMemoryTools.memoryStrReplace("profile.md", "type: USER\n", "");

        assertThat(stored("profile")
            .getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
    }

    @Test
    void testInsertingBelowTheFrontmatterAppendsToTheBody() {
        assertThat(autoMemoryTools.memoryInsert("profile.md", 7, "Uses British spelling."))
            .startsWith("Successfully inserted text at line 7");

        AiAutoMemory memory = stored("profile");

        assertThat(memory.getContent()).isEqualTo("Prefers concise replies.\nUses British spelling.");
        assertThat(memory.getTitle()).isEqualTo("Profile");
    }

    @Test
    void testInsertingAboveTheFrontmatterIsRejectedAndChangesNothing() {
        String result = autoMemoryTools.memoryInsert("profile.md", 0, "stray line");

        assertThat(result).startsWith("Error: The entry has no frontmatter block");

        AiAutoMemory memory = stored("profile");

        assertThat(memory.getContent()).isEqualTo("Prefers concise replies.");
        assertThat(memory.getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
    }

    @Test
    void testAnUnknownTypeIsRejectedAndChangesNothing() {
        String result = autoMemoryTools.memoryStrReplace("profile.md", "type: USER", "type: preference");

        assertThat(result).contains("Unknown memory type 'preference'");
        assertThat(stored("profile")
            .getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
    }

    @Test
    void testChangingTheNameLineIsRejected() {
        String result = autoMemoryTools.memoryStrReplace("profile.md", "name: profile", "name: other");

        assertThat(result).contains("Use MemoryRename");
    }

    @Test
    void testAPathThatIsNotASlugIsRejectedWithTheAllowedCharacters() {
        String result = autoMemoryTools.memoryCreate("notes/today.md", PROFILE_ENTRY);

        assertThat(result).startsWith("Error: Invalid entry name 'notes/today'");
    }

    @Test
    void testAnEditOfAnEntryChangedSinceItWasReadIsRejected() throws IOException {
        AutoMemoryResource resource = new AutoMemoryResource(aiAutoMemoryService, OWNER, "profile");

        String rendered = read(resource);

        aiAutoMemoryService.update(
            OWNER, "profile", 0L, new AiAutoMemoryPatch(null, null, null, "Changed by another run."));

        assertThatThrownBy(() -> write(resource, rendered.replace("concise", "short")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("was changed by someone else");
        assertThat(stored("profile")
            .getContent()).isEqualTo("Changed by another run.");
    }

    @Test
    void testAnEditOfAnEntryDeletedSinceItWasReadIsRejected() throws IOException {
        AutoMemoryResource resource = new AutoMemoryResource(aiAutoMemoryService, OWNER, "profile");

        String rendered = read(resource);

        aiAutoMemoryService.delete(OWNER, "profile");

        assertThatThrownBy(() -> write(resource, rendered))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("was deleted by someone else");
        assertThat(stored("profile")).isNull();
    }

    @Test
    void testACreateDoesNotOverwriteAnEntryCreatedMeanwhile() {
        AutoMemoryResource resource = new AutoMemoryResource(aiAutoMemoryService, OWNER, "profile");

        assertThatThrownBy(() -> write(resource, PROFILE_ENTRY.replace("concise", "verbose")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("already exists");
        assertThat(stored("profile")
            .getContent()).isEqualTo("Prefers concise replies.");
    }

    @Test
    void testAValueTheServiceRejectsIsReportedAsAnIoException() throws IOException {
        AutoMemoryResource resource = new AutoMemoryResource(aiAutoMemoryService, OWNER, "profile");

        String rendered = read(resource);

        assertThatThrownBy(() -> write(resource, rendered.replace("title: Profile", "title: " + "x".repeat(256))))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("title must be at most 255 characters");
        assertThat(stored("profile")
            .getTitle()).isEqualTo("Profile");
    }

    @Test
    void testAnEditOfAnEntryReplacedByANewOneWithTheSameNameIsRejected() throws IOException {
        AutoMemoryResource resource = new AutoMemoryResource(aiAutoMemoryService, OWNER, "profile");

        String rendered = read(resource);

        aiAutoMemoryService.delete(OWNER, "profile");
        aiAutoMemoryService.create(OWNER, "profile", "Profile", null, AiAutoMemoryType.USER, "A new memory.");

        assertThatThrownBy(() -> write(resource, rendered.replace("concise", "short")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("was deleted by someone else");
        assertThat(stored("profile")
            .getContent()).isEqualTo("A new memory.");
    }

    @Test
    void testClosingTheOutputStreamTwiceWritesOnce() throws IOException {
        AutoMemoryResource resource = new AutoMemoryResource(aiAutoMemoryService, OWNER, "profile");

        String rendered = read(resource);

        OutputStream outputStream = resource.getOutputStream();

        outputStream.write(rendered.replace("concise", "short")
            .getBytes(StandardCharsets.UTF_8));

        outputStream.close();
        outputStream.close();

        AiAutoMemory memory = stored("profile");

        assertThat(memory.getContent()).isEqualTo("Prefers short replies.");
        assertThat(memory.getVersion()).isEqualTo(1L);
    }

    @Test
    void testASecondWriteOnTheSameResourceBuildsOnTheFirst() throws IOException {
        AutoMemoryResource resource = new AutoMemoryResource(aiAutoMemoryService, OWNER, "notes");

        write(resource, PROFILE_ENTRY);
        write(resource, PROFILE_ENTRY.replace("concise", "short"));

        AiAutoMemory memory = stored("notes");

        assertThat(memory.getContent()).isEqualTo("Prefers short replies.");
        assertThat(memory.getVersion()).isEqualTo(1L);
    }

    @Test
    void testRenameMovesTheEntryAndKeepsItsContent() {
        assertThat(autoMemoryTools.memoryRename("profile.md", "user_profile.md")).startsWith("Successfully renamed");

        assertThat(stored("profile")).isNull();
        assertThat(stored("user_profile").getContent()).isEqualTo("Prefers concise replies.");
        assertThat(autoMemoryTools.memoryView("", null)).contains("- user_profile.md — [USER] Profile");
    }

    @Test
    void testRenameOntoAnExistingEntryIsRejectedAndChangesNothing() {
        assertThat(autoMemoryTools.memoryCreate("notes.md", PROFILE_ENTRY.replace("concise", "detailed")))
            .startsWith("Successfully created");

        assertThat(autoMemoryTools.memoryRename("profile.md", "notes.md"))
            .isEqualTo("Error: Destination path already exists: notes.md");

        assertThat(stored("profile").getContent()).isEqualTo("Prefers concise replies.");
        assertThat(stored("notes").getContent()).isEqualTo("Prefers detailed replies.");
    }

    @Test
    void testDeleteRemovesTheEntry() {
        assertThat(autoMemoryTools.memoryDelete("profile.md")).startsWith("Successfully deleted");

        assertThat(stored("profile")).isNull();
        assertThat(autoMemoryTools.memoryView("", null))
            .isEqualTo("MEMORY index is empty. Create entries with MemoryCreate.");
    }

    @Test
    void testAnEditOfAMemoryWithALegacyMultiLineDescriptionRepairsIt() throws IOException {
        FileStorageAiAutoMemoryRepository repository = new FileStorageAiAutoMemoryRepository(
            new FilesystemFileStorageService(tempDir.toString()));

        AiAutoMemory profile = stored("profile");

        repository.save(
            AiAutoMemory.restore(
                OWNER, profile.getId(), "profile", "Profile", "first\n---\nsecond", AiAutoMemoryType.USER,
                "Prefers concise replies.", profile.getCreatedAt(), profile.getUpdatedAt(), profile.getVersion()));

        assertThat(autoMemoryTools.memoryStrReplace("profile.md", "concise", "short"))
            .startsWith("Successfully edited");

        AiAutoMemory memory = stored("profile");

        assertThat(memory.getDescription()).isEqualTo("first --- second");
        assertThat(memory.getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(memory.getContent()).isEqualTo("Prefers short replies.");
    }

    @Test
    void testResourcesForTheSameNameUnderDifferentOwnersAreNotEqual() {
        AutoMemoryResource resource = new AutoMemoryResource(aiAutoMemoryService, OWNER, "profile");

        assertThat(resource).isEqualTo(new AutoMemoryResource(aiAutoMemoryService, OWNER, "profile"));
        assertThat(resource).isNotEqualTo(new AutoMemoryResource(aiAutoMemoryService, OTHER_OWNER, "profile"));
    }

    @Test
    void testTheIndexNameCannotBeUsedForAnEntry() {
        assertThat(autoMemoryTools.memoryCreate("memory", PROFILE_ENTRY))
            .startsWith("Error: The entry name 'memory' is reserved for the index");
        assertThat(autoMemoryTools.memoryRename("profile.md", "memory"))
            .startsWith("Error: The entry name 'memory' is reserved for the index");

        assertThat(stored("memory")).isNull();
        assertThat(stored("profile")).isNotNull();
    }

    @Test
    void testToolsBuiltForAnotherOwnerNeitherSeeNorChangeThisOwnersEntries() {
        AutoMemoryTools otherOwnerTools = new AutoMemoryTools(
            new AutoMemoryResourceResolver(aiAutoMemoryService, OTHER_OWNER),
            new ServiceBackedAutoMemoryDirectoryOps(aiAutoMemoryService, OTHER_OWNER));

        assertThat(otherOwnerTools.memoryView("profile.md", null)).startsWith("Error: Path does not exist");
        assertThat(otherOwnerTools.memoryCreate("profile.md", PROFILE_ENTRY.replace("concise", "detailed")))
            .startsWith("Successfully created");
        assertThat(otherOwnerTools.memoryStrReplace("profile.md", "detailed", "long"))
            .startsWith("Successfully edited");

        assertThat(stored("profile")
            .getContent()).isEqualTo("Prefers concise replies.");
        assertThat(stored(OTHER_OWNER, "profile")
            .getContent()).isEqualTo("Prefers long replies.");
    }

    private AiAutoMemory stored(String name) {
        return stored(OWNER, name);
    }

    private AiAutoMemory stored(AiAutoMemoryOwner owner, String name) {
        return aiAutoMemoryService.read(owner, name)
            .orElse(null);
    }

    private static String read(AutoMemoryResource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void write(AutoMemoryResource resource, String text) throws IOException {
        try (OutputStream outputStream = resource.getOutputStream()) {
            outputStream.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
