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

package com.bytechef.automation.assetfile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.assetfile.config.AssetFileIntTestConfiguration;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.repository.AssetFileRepository;
import com.bytechef.automation.assetfile.repository.WorkspaceAssetFileRepository;
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
 * Integration tests for {@link AssetFileFacade}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AssetFileIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AssetFileFacadeIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AssetFileFacade assetFileFacade;

    @Autowired
    private AssetFileRepository assetFileRepository;

    @Autowired
    private WorkspaceAssetFileRepository workspaceAssetFileRepository;

    private Long workspaceId;

    @BeforeEach
    public void beforeEach() {
        workspaceAssetFileRepository.deleteAll();
        assetFileRepository.deleteAll();

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
        workspaceAssetFileRepository.deleteAll();
        assetFileRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testUploadDownloadRoundTrip() throws Exception {
        byte[] contentBytes = "# Heading\nHello".getBytes(StandardCharsets.UTF_8);

        AssetFile created = assetFileFacade.createFromUpload(
            workspaceId, 0, "doc.md", "text/markdown", new ByteArrayInputStream(contentBytes));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("doc.md");
        assertThat(created.getSizeBytes()).isEqualTo(contentBytes.length);

        List<AssetFile> listed = assetFileFacade.findAllByWorkspaceIdAndEnvironment(workspaceId, 0, null);

        assertThat(listed).hasSize(1);
        assertThat(listed.get(0)
            .getId()).isEqualTo(created.getId());

        try (InputStream downloaded = assetFileFacade.downloadContent(created.getId())) {
            byte[] downloadedBytes = downloaded.readAllBytes();

            assertThat(downloadedBytes).isEqualTo(contentBytes);
        }
    }

    @Test
    void testQuotaEnforcedAtWorkspaceLevel() {
        byte[] first = new byte[900];

        assetFileFacade.createFromUpload(
            workspaceId, 0, "a.bin", "application/octet-stream", new ByteArrayInputStream(first));

        byte[] second = new byte[1000];

        assertThatThrownBy(() -> assetFileFacade.createFromUpload(
            workspaceId, 0, "b.bin", "application/octet-stream", new ByteArrayInputStream(second)))
                .isInstanceOf(AssetFileQuotaExceededException.class);
    }

    @Test
    void testDeleteRemovesBlobAndRow() {
        byte[] contentBytes = "payload".getBytes(StandardCharsets.UTF_8);

        AssetFile created = assetFileFacade.createFromUpload(
            workspaceId, 0, "removable.txt", "text/plain", new ByteArrayInputStream(contentBytes));

        Long createdId = created.getId();

        assertThat(assetFileRepository.findById(createdId)).isPresent();

        assetFileFacade.delete(createdId);

        assertThat(assetFileRepository.findById(createdId)).isNotPresent();

        assertThatThrownBy(() -> assetFileFacade.downloadContent(createdId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRenameCollisionSuffixes() {
        byte[] firstBytes = "first".getBytes(StandardCharsets.UTF_8);

        AssetFile first = assetFileFacade.createFromUpload(
            workspaceId, 0, "foo.md", "text/markdown", new ByteArrayInputStream(firstBytes));

        byte[] secondBytes = "second".getBytes(StandardCharsets.UTF_8);

        AssetFile second = assetFileFacade.createFromUpload(
            workspaceId, 0, "foo.md", "text/markdown", new ByteArrayInputStream(secondBytes));

        assertThat(first.getName()).isEqualTo("foo.md");
        assertThat(second.getName()).isEqualTo("foo-2.md");
    }

    /**
     * Pins the multi-tenancy invariant: a {@code findAllByWorkspaceIdAndEnvironment(ws, 0, ...)} call must NOT return
     * rows whose {@code environment} column is non-zero, even within the same workspace. The migration adds an indexed
     * {@code environment} column and the repository queries gain a {@code wf.environment = :environment} predicate;
     * this test exercises that predicate end-to-end through the facade so a future regression that drops the predicate
     * (or applies it to the wrong join) is caught at the integration layer rather than only in the unit-level repo
     * mock. Also pins the related quota invariant — quota is summed per (workspace, environment), so a workspace that
     * sits at the limit in DEVELOPMENT must still be able to upload in STAGING.
     */
    @Test
    void testFindAllIsolatesByEnvironment() {
        AssetFile devFile = assetFileFacade.createFromUpload(
            workspaceId, 0, "dev.md", "text/markdown",
            new ByteArrayInputStream("dev content".getBytes(StandardCharsets.UTF_8)));

        AssetFile stagingFile = assetFileFacade.createFromUpload(
            workspaceId, 1, "staging.md", "text/markdown",
            new ByteArrayInputStream("staging content".getBytes(StandardCharsets.UTF_8)));

        AssetFile prodFile = assetFileFacade.createFromUpload(
            workspaceId, 2, "prod.md", "text/markdown",
            new ByteArrayInputStream("prod content".getBytes(StandardCharsets.UTF_8)));

        // Listing each environment returns exactly its own row, never the others.
        List<AssetFile> devListing = assetFileFacade.findAllByWorkspaceIdAndEnvironment(workspaceId, 0, null);
        List<AssetFile> stagingListing = assetFileFacade.findAllByWorkspaceIdAndEnvironment(workspaceId, 1, null);
        List<AssetFile> prodListing = assetFileFacade.findAllByWorkspaceIdAndEnvironment(workspaceId, 2, null);

        assertThat(devListing).extracting(AssetFile::getId)
            .containsExactly(devFile.getId());
        assertThat(stagingListing).extracting(AssetFile::getId)
            .containsExactly(stagingFile.getId());
        assertThat(prodListing).extracting(AssetFile::getId)
            .containsExactly(prodFile.getId());

        // The persisted environment column on each row matches what was supplied at create-time. Guards against a
        // future regression where the facade forgot to call setEnvironment(...) and the row defaulted to ordinal 0.
        assertThat(devFile.getEnvironmentId()).isEqualTo(0L);
        assertThat(stagingFile.getEnvironmentId()).isEqualTo(1L);
        assertThat(prodFile.getEnvironmentId()).isEqualTo(2L);

        // Same-name uniqueness is per-environment: creating "dev.md" in STAGING does NOT collide with "dev.md" in
        // DEVELOPMENT, so neither file ends up renamed to "dev-2.md".
        AssetFile sameNameInStaging = assetFileFacade.createFromUpload(
            workspaceId, 1, "dev.md", "text/markdown",
            new ByteArrayInputStream("not a collision".getBytes(StandardCharsets.UTF_8)));

        assertThat(sameNameInStaging.getName()).isEqualTo("dev.md");
    }
}
