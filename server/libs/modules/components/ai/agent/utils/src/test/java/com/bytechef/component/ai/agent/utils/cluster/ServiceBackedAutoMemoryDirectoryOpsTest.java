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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryConcurrentModificationException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.ai.auto.memory.DuplicateAiAutoMemoryNameException;
import com.bytechef.platform.configuration.domain.Environment;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ServiceBackedAutoMemoryDirectoryOpsTest {

    private static final AiAutoMemoryOwner OWNER = new AiAutoMemoryOwner(
        1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 100L, Environment.DEVELOPMENT);

    private final AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);
    private final ServiceBackedAutoMemoryDirectoryOps directoryOps =
        new ServiceBackedAutoMemoryDirectoryOps(aiAutoMemoryService, OWNER);

    @Test
    void testListUsesTheFixedOwner() {
        when(aiAutoMemoryService.list(OWNER, null)).thenReturn(List.<AiAutoMemory>of());

        String index = directoryOps.list("MEMORY.md");

        assertThat(index).isEqualTo("MEMORY index is empty. Create entries with MemoryCreate.");

        verify(aiAutoMemoryService).list(OWNER, null);
    }

    /**
     * The model learns entry paths from the {@code <name>.md} token of each index line, and the MemoryView tool
     * description promises this format, so it is pinned exactly — in the service's newest-first order, with the
     * description part left out when there is none.
     */
    @Test
    void testListRendersOneLinePerMemoryInTheServiceOrder() {
        AiAutoMemory newer = AiAutoMemory.restore(
            OWNER, 2L, "user_profile", "Profile", "What the user prefers", AiAutoMemoryType.USER, "body", null, null,
            0L);
        AiAutoMemory older = AiAutoMemory.restore(
            OWNER, 1L, "deadline", "Release date", null, AiAutoMemoryType.PROJECT, "body", null, null, 0L);

        when(aiAutoMemoryService.list(OWNER, null)).thenReturn(List.of(newer, older));

        assertThat(directoryOps.list("")).isEqualTo(
            """
                MEMORY index (2 entries):
                - user_profile.md — [USER] Profile — What the user prefers
                - deadline.md — [PROJECT] Release date
                """);
    }

    @Test
    void testDeleteDerivesTheMemoryNameFromThePath() throws IOException {
        directoryOps.delete(" /Notes.MD ");

        verify(aiAutoMemoryService).delete(OWNER, "notes");
    }

    @Test
    void testRenameDerivesBothMemoryNamesFromThePaths() throws IOException {
        directoryOps.rename("old.md", "New-Name.md");

        verify(aiAutoMemoryService).rename(OWNER, "old", "new-name");
    }

    @Test
    void testDeleteOfAMissingMemoryIsReportedAsAnIoFailure() {
        when(aiAutoMemoryService.delete(OWNER, "gone")).thenThrow(new AiAutoMemoryNotFoundException("Memory"));

        assertThatThrownBy(() -> directoryOps.delete("gone.md"))
            .isInstanceOf(IOException.class)
            .hasMessage("Memory entry does not exist: gone.md");
    }

    @Test
    void testRenameOntoAnExistingNameIsReportedAsAnIoFailure() {
        when(aiAutoMemoryService.rename(OWNER, "old", "taken"))
            .thenThrow(new DuplicateAiAutoMemoryNameException("taken"));

        assertThatThrownBy(() -> directoryOps.rename("old.md", "taken.md"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("'taken' already exists");
    }

    @Test
    void testRenameOfAMissingMemoryIsReportedAsAnIoFailure() {
        when(aiAutoMemoryService.rename(OWNER, "gone", "new")).thenThrow(new AiAutoMemoryNotFoundException("Memory"));

        assertThatThrownBy(() -> directoryOps.rename("gone.md", "new.md"))
            .isInstanceOf(IOException.class)
            .hasMessage("Memory entry does not exist: gone.md");
    }

    /**
     * A conflict is something the model can fix by reading the entry again, so it must surface as the actionable
     * failure rather than the generic "operation failed" the tools report for anything unexpected.
     */
    @Test
    void testDeleteOfAMemoryChangedMeanwhileIsReportedAsAnIoFailure() {
        when(aiAutoMemoryService.delete(OWNER, "notes"))
            .thenThrow(new AiAutoMemoryConcurrentModificationException("notes"));

        assertThatThrownBy(() -> directoryOps.delete("notes.md"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("changed by someone else");
    }

    @Test
    void testRenameOfAMemoryChangedMeanwhileIsReportedAsAnIoFailure() {
        when(aiAutoMemoryService.rename(OWNER, "notes", "renamed"))
            .thenThrow(new AiAutoMemoryConcurrentModificationException("notes"));

        assertThatThrownBy(() -> directoryOps.rename("notes.md", "renamed.md"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("changed by someone else");
    }

    @Test
    void testRenameToANameThatIsNotASlugIsRejectedWithoutReachingTheService() {
        assertThatThrownBy(() -> directoryOps.rename("old.md", "my notes.md"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Invalid entry name 'my notes'");

        verify(aiAutoMemoryService, never()).rename(any(), any(), any());
    }

    @Test
    void testAMissingOwnerIsRejectedAtConstruction() {
        assertThatThrownBy(() -> new ServiceBackedAutoMemoryDirectoryOps(aiAutoMemoryService, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("owner");
    }
}
