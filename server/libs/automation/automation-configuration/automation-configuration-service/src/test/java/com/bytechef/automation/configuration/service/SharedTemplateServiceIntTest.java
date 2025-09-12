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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.SharedTemplate;
import com.bytechef.automation.configuration.repository.SharedTemplateRepository;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class SharedTemplateServiceIntTest {

    @Autowired
    private SharedTemplateService sharedTemplateService;

    @Autowired
    private SharedTemplateRepository sharedTemplateRepository;

    @AfterEach
    public void afterEach() {
        sharedTemplateRepository.deleteAll();
    }

    private FileEntry createTestFileEntry(String name) {
        return new FileEntry(name, "json", "application/json", "http://test-url/" + name.hashCode());
    }

    @Test
    void testFetchSharedTemplateWithExistingUuid() {
        // Given
        FileEntry fileEntry = createTestFileEntry("test-template.json");

        SharedTemplate templateToSave = SharedTemplate.builder()
            .uuid(UUID.randomUUID())
            .template(fileEntry)
            .build();

        SharedTemplate savedTemplate = sharedTemplateRepository.save(templateToSave);

        UUID uuid = savedTemplate.getUuid();

        // When
        Optional<SharedTemplate> result = sharedTemplateService.fetchSharedTemplate(uuid);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()
            .getId()).isEqualTo(savedTemplate.getId());
        assertThat(result.get()
            .getUuid()).isEqualTo(savedTemplate.getUuid());
        assertThat(result.get()
            .getTemplate()).isEqualTo(fileEntry);
    }

    @Test
    void testFetchSharedTemplateWithNonExistentUuid() {
        // Given
        UUID nonExistentUuid = UUID.randomUUID();

        // When
        Optional<SharedTemplate> result = sharedTemplateService.fetchSharedTemplate(nonExistentUuid);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testGetSharedTemplateWithExistingUuid() {
        // Given
        FileEntry fileEntry = createTestFileEntry("test-template.json");

        SharedTemplate templateToSave = SharedTemplate.builder()
            .uuid(UUID.randomUUID())
            .template(fileEntry)
            .build();

        SharedTemplate savedTemplate = sharedTemplateRepository.save(templateToSave);

        UUID uuid = savedTemplate.getUuid();

        // When
        SharedTemplate result = sharedTemplateService.getSharedTemplate(uuid);

        // Then
        assertThat(result.getId()).isEqualTo(savedTemplate.getId());
        assertThat(result.getUuid()).isEqualTo(savedTemplate.getUuid());
        assertThat(result.getTemplate()).isEqualTo(fileEntry);
    }

    @Test
    void testGetSharedTemplateWithNonExistentUuid() {
        // Given
        UUID nonExistentUuid = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> sharedTemplateService.getSharedTemplate(nonExistentUuid))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSaveWithExistingUuid() {
        // Given
        FileEntry originalFileEntry = createTestFileEntry("original-template.json");
        FileEntry updatedFileEntry = createTestFileEntry("updated-template.json");

        SharedTemplate templateToSave = SharedTemplate.builder()
            .uuid(UUID.randomUUID())
            .template(originalFileEntry)
            .build();

        SharedTemplate existingTemplate = sharedTemplateRepository.save(templateToSave);

        UUID uuid = existingTemplate.getUuid();

        // When
        SharedTemplate result = sharedTemplateService.save(uuid, updatedFileEntry);

        // Then
        assertThat(result.getUuid()).isEqualTo(existingTemplate.getUuid());
        assertThat(result.getTemplate()).isEqualTo(updatedFileEntry);
        assertThat(result.getId()).isEqualTo(existingTemplate.getId());
        assertThat(result.getLastModifiedDate()).isNotNull();

        // Verify the template was updated in the repository
        Optional<SharedTemplate> savedTemplate = sharedTemplateRepository.findByUuid(existingTemplate.getUuid());
        assertThat(savedTemplate).isPresent();
        assertThat(savedTemplate.get()
            .getTemplate()).isEqualTo(updatedFileEntry);
    }

    @Test
    void testSaveWithNullUuid() {
        // Given
        FileEntry fileEntry = createTestFileEntry("test-template.json");

        // When & Then
        assertThatThrownBy(() -> sharedTemplateService.save(null, fileEntry))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'uuid' must not be null");
    }

    @Test
    void testSaveWithNullFileEntry() {
        // Given
        UUID uuid = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> sharedTemplateService.save(uuid, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'template' must not be null");
    }

    @Test
    void testUpdateWithExistingTemplate() {
        // Given
        FileEntry originalFileEntry = createTestFileEntry("original-template.json");
        FileEntry updatedFileEntry = createTestFileEntry("updated-template.json");

        SharedTemplate templateToSave = SharedTemplate.builder()
            .uuid(UUID.randomUUID())
            .template(originalFileEntry)
            .build();

        SharedTemplate existingTemplate = sharedTemplateRepository.save(templateToSave);

        SharedTemplate templateToUpdate = SharedTemplate.builder()
            .id(existingTemplate.getId())
            .template(updatedFileEntry)
            .build();
        templateToUpdate.setUuid(existingTemplate.getUuid()); // Set UUID from existing template

        // When
        SharedTemplate result = sharedTemplateService.update(templateToUpdate);

        // Then
        assertThat(result.getId()).isEqualTo(existingTemplate.getId());
        assertThat(result.getTemplate()).isEqualTo(updatedFileEntry);
        assertThat(result.getLastModifiedDate()).isNotNull();

        // Verify the template was updated in the repository
        Optional<SharedTemplate> savedTemplate = sharedTemplateRepository.findById(existingTemplate.getId());
        assertThat(savedTemplate).isPresent();
        assertThat(savedTemplate.get()
            .getTemplate()).isEqualTo(updatedFileEntry);
    }

    @Test
    void testUpdateWithNullSharedTemplate() {
        // When & Then
        assertThatThrownBy(() -> sharedTemplateService.update(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'sharedTemplate' must not be null");
    }

    @Test
    void testUpdateWithNullId() {
        // Given
        FileEntry fileEntry = createTestFileEntry("test-template.json");
        SharedTemplate template = SharedTemplate.builder()
            .template(fileEntry)
            .build();

        // When & Then
        assertThatThrownBy(() -> sharedTemplateService.update(template))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'id' must not be null");
    }

    @Test
    void testUpdateWithNullTemplate() {
        // Given
        SharedTemplate template = SharedTemplate.builder()
            .id(1L)
            .build();

        // When & Then
        assertThatThrownBy(() -> sharedTemplateService.update(template))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'uuid' must not be null");
    }

    @Test
    void testUpdateWithNonExistentId() {
        // Given
        FileEntry fileEntry = createTestFileEntry("test-template.json");

        SharedTemplate sharedTemplate = SharedTemplate.builder()
            .id(99999L)
            .template(fileEntry)
            .uuid(UUID.randomUUID())
            .build();

        // When & Then
        assertThatThrownBy(() -> sharedTemplateService.update(sharedTemplate))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
