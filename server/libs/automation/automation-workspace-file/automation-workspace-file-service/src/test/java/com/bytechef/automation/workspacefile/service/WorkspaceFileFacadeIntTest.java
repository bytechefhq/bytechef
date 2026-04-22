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

package com.bytechef.automation.workspacefile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.workspacefile.config.WorkspaceFileIntTestConfiguration;
import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.exception.WorkspaceFileQuotaExceededException;
import com.bytechef.automation.workspacefile.repository.WorkspaceFileRepository;
import com.bytechef.automation.workspacefile.repository.WorkspaceWorkspaceFileRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for {@link WorkspaceFileFacade}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkspaceFileIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class WorkspaceFileFacadeIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkspaceFileFacade workspaceFileFacade;

    @Autowired
    private WorkspaceFileRepository workspaceFileRepository;

    @Autowired
    private WorkspaceWorkspaceFileRepository workspaceWorkspaceFileRepository;

    private Long workspaceId;

    @BeforeEach
    public void beforeEach() {
        workspaceWorkspaceFileRepository.deleteAll();
        workspaceFileRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM workspace");

        Timestamp now = Timestamp.from(Instant.now());

        jdbcTemplate.update(
            "INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
            "test-workspace", now, "tester", now, "tester", 0);

        workspaceId = Objects.requireNonNull(
            jdbcTemplate.queryForObject("SELECT id FROM workspace WHERE name = ?", Long.class, "test-workspace"));
    }

    @AfterEach
    public void afterEach() {
        workspaceWorkspaceFileRepository.deleteAll();
        workspaceFileRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testUploadDownloadRoundTrip() throws Exception {
        byte[] contentBytes = "# Heading\nHello".getBytes(StandardCharsets.UTF_8);

        WorkspaceFile created = workspaceFileFacade.createFromUpload(
            workspaceId, "doc.md", "text/markdown", new ByteArrayInputStream(contentBytes));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("doc.md");
        assertThat(created.getSizeBytes()).isEqualTo(contentBytes.length);

        List<WorkspaceFile> listed = workspaceFileFacade.findAllByWorkspaceId(workspaceId, null);

        assertThat(listed).hasSize(1);
        assertThat(listed.get(0)
            .getId()).isEqualTo(created.getId());

        try (InputStream downloaded = workspaceFileFacade.downloadContent(created.getId())) {
            byte[] downloadedBytes = downloaded.readAllBytes();

            assertThat(downloadedBytes).isEqualTo(contentBytes);
        }
    }

    @Test
    void testQuotaEnforcedAtWorkspaceLevel() {
        byte[] first = new byte[900];

        workspaceFileFacade.createFromUpload(
            workspaceId, "a.bin", "application/octet-stream", new ByteArrayInputStream(first));

        byte[] second = new byte[1000];

        assertThatThrownBy(() -> workspaceFileFacade.createFromUpload(
            workspaceId, "b.bin", "application/octet-stream", new ByteArrayInputStream(second)))
                .isInstanceOf(WorkspaceFileQuotaExceededException.class);
    }

    @Test
    void testDeleteRemovesBlobAndRow() {
        byte[] contentBytes = "payload".getBytes(StandardCharsets.UTF_8);

        WorkspaceFile created = workspaceFileFacade.createFromUpload(
            workspaceId, "removable.txt", "text/plain", new ByteArrayInputStream(contentBytes));

        Long createdId = created.getId();

        assertThat(workspaceFileRepository.findById(createdId)).isPresent();

        workspaceFileFacade.delete(createdId);

        assertThat(workspaceFileRepository.findById(createdId)).isNotPresent();

        assertThatThrownBy(() -> workspaceFileFacade.downloadContent(createdId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRenameCollisionSuffixes() {
        byte[] firstBytes = "first".getBytes(StandardCharsets.UTF_8);

        WorkspaceFile first = workspaceFileFacade.createFromUpload(
            workspaceId, "foo.md", "text/markdown", new ByteArrayInputStream(firstBytes));

        byte[] secondBytes = "second".getBytes(StandardCharsets.UTF_8);

        WorkspaceFile second = workspaceFileFacade.createFromUpload(
            workspaceId, "foo.md", "text/markdown", new ByteArrayInputStream(secondBytes));

        assertThat(first.getName()).isEqualTo("foo.md");
        assertThat(second.getName()).isEqualTo("foo-2.md");
    }
}
