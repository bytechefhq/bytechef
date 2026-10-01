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

package com.bytechef.atlas.workflow.repository.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations.HeadFiles;
import com.bytechef.atlas.configuration.repository.git.operations.JGitWorkflowOperations;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;

/**
 * @author Ivica Cardic
 */
class JGitWorkflowOperationsTest {

    private static final String BRANCH = "main";

    @TempDir
    private Path tempDirectory;

    private JGitWorkflowOperations jGitWorkflowOperations;

    @BeforeEach
    void beforeEach() throws Exception {
        File originDirectory = tempDirectory.resolve("origin.git")
            .toFile();

        Git.init()
            .setBare(true)
            .setInitialBranch(BRANCH)
            .setDirectory(originDirectory)
            .call()
            .close();

        File seedDirectory = tempDirectory.resolve("seed")
            .toFile();

        try (Git seed = Git.cloneRepository()
            .setURI(originDirectory.toURI()
                .toString())
            .setDirectory(seedDirectory)
            .call()) {

            Files.writeString(seedDirectory.toPath()
                .resolve("README.md"), "seed");

            seed.add()
                .addFilepattern(".")
                .call();
            seed.commit()
                .setMessage("seed")
                .call();
            seed.push()
                .setRefSpecs(new RefSpec("HEAD:refs/heads/" + BRANCH))
                .call();
        }

        jGitWorkflowOperations = new JGitWorkflowOperations(
            originDirectory.toURI()
                .toString(),
            BRANCH, List.of("json", "yaml", "yml"), List.of(), List.of("agents/"), "user", "password");
    }

    @Test
    void testWriteAndReadContentFilesNextToWorkflows() {
        byte[] agentFile = "{\"title\":\"Support Bot\"}".getBytes(StandardCharsets.UTF_8);

        jGitWorkflowOperations.write(
            List.of(workflowResource("Ordinary", "{\"label\":\"Ordinary\",\"tasks\":[]}")),
            Map.of("agents/support-bot.json", agentFile), "push");

        HeadFiles headFiles = jGitWorkflowOperations.getHeadFiles();

        assertThat(headFiles.workflowResources())
            .as("the agent file is not read as a workflow")
            .hasSize(1);
        assertThat(headFiles.contentFiles()).containsOnlyKeys("agents/support-bot.json");
        assertThat(headFiles.contentFiles()
            .get("agents/support-bot.json")).isEqualTo(agentFile);
    }

    @Test
    void testWriteRefusesAContentFileOutsideTheContentDirectories() {
        Map<String, byte[]> contentFiles = Map.of("agents/../escape.json", new byte[0]);

        assertThatThrownBy(() -> jGitWorkflowOperations.write(List.of(), contentFiles, "push"))
            .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    private static WorkflowResource workflowResource(String label, String definition) {
        ByteArrayResource resource = new ByteArrayResource(definition.getBytes(StandardCharsets.UTF_8)) {

            @Override
            public String getFilename() {
                return label + ".json";
            }
        };

        return new WorkflowResource(label, Map.of(), resource, Format.JSON);
    }
}
