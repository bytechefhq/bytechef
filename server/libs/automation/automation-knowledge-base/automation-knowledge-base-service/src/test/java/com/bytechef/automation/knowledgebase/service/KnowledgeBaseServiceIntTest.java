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

package com.bytechef.automation.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.knowledgebase.config.KnowledgeBaseIntTestConfiguration;
import com.bytechef.automation.knowledgebase.config.KnowledgeBaseIntTestConfigurationSharedMocks;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for {@link KnowledgeBaseService}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@KnowledgeBaseIntTestConfigurationSharedMocks
class KnowledgeBaseServiceIntTest {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @BeforeEach
    public void beforeEach() {
        knowledgeBaseRepository.deleteAll();
    }

    @AfterEach
    public void afterEach() {
        knowledgeBaseRepository.deleteAll();
    }

    @Test
    void testCreateKnowledgeBase() {
        KnowledgeBase knowledgeBase = createKnowledgeBase("Test KnowledgeBase");

        KnowledgeBase savedKnowledgeBase = knowledgeBaseService.createKnowledgeBase(knowledgeBase);

        assertThat(savedKnowledgeBase.getId()).isNotNull();
        assertThat(savedKnowledgeBase.getName()).isEqualTo("Test KnowledgeBase");
        assertThat(savedKnowledgeBase.getCreatedDate()).isNotNull();
    }

    @Test
    void testGetKnowledgeBase() {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.save(createKnowledgeBase("Test KnowledgeBase"));

        KnowledgeBase retrievedKnowledgeBase = knowledgeBaseService.getKnowledgeBase(knowledgeBase.getId());

        assertThat(retrievedKnowledgeBase).isNotNull();
        assertThat(retrievedKnowledgeBase.getId()).isEqualTo(knowledgeBase.getId());
        assertThat(retrievedKnowledgeBase.getName()).isEqualTo("Test KnowledgeBase");
    }

    @Test
    void testGetKnowledgeBaseNotFound() {
        assertThatThrownBy(() -> knowledgeBaseService.getKnowledgeBase(Long.MAX_VALUE))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("KnowledgeBase not found");
    }

    @Test
    void testGetKnowledgeBases() {
        knowledgeBaseRepository.save(createKnowledgeBase("KnowledgeBase 1"));
        knowledgeBaseRepository.save(createKnowledgeBase("KnowledgeBase 2"));
        knowledgeBaseRepository.save(createKnowledgeBase("KnowledgeBase 3"));

        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getKnowledgeBases();

        assertThat(knowledgeBases).hasSize(3);
    }

    @Test
    void testUpdateKnowledgeBase() {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.save(createKnowledgeBase("Original Name"));

        KnowledgeBase updateData = new KnowledgeBase();

        updateData.setName("Updated Name");
        updateData.setDescription("Updated Description");
        updateData.setMaxChunkSize(1000);
        updateData.setMinChunkSizeChars(100);
        updateData.setOverlap(50);

        KnowledgeBase updatedKnowledgeBase =
            knowledgeBaseService.updateKnowledgeBase(knowledgeBase.getId(), updateData);

        assertThat(updatedKnowledgeBase.getId()).isEqualTo(knowledgeBase.getId());
        assertThat(updatedKnowledgeBase.getName()).isEqualTo("Updated Name");
        assertThat(updatedKnowledgeBase.getDescription()).isEqualTo("Updated Description");
        assertThat(updatedKnowledgeBase.getMaxChunkSize()).isEqualTo(1000);
        assertThat(updatedKnowledgeBase.getMinChunkSizeChars()).isEqualTo(100);
        assertThat(updatedKnowledgeBase.getOverlap()).isEqualTo(50);
    }

    @Test
    void testDeleteKnowledgeBase() {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.save(createKnowledgeBase("Test KnowledgeBase"));

        assertThat(knowledgeBaseRepository.findById(knowledgeBase.getId())).isPresent();

        knowledgeBaseService.deleteKnowledgeBase(knowledgeBase.getId());

        assertThat(knowledgeBaseRepository.findById(knowledgeBase.getId())).isNotPresent();
    }

    @Test
    void testCreateKnowledgeBaseWithAllFields() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Full KnowledgeBase");
        knowledgeBase.setDescription("A comprehensive knowledge base");
        knowledgeBase.setMaxChunkSize(2048);
        knowledgeBase.setMinChunkSizeChars(256);
        knowledgeBase.setOverlap(128);

        KnowledgeBase savedKnowledgeBase = knowledgeBaseService.createKnowledgeBase(knowledgeBase);

        assertThat(savedKnowledgeBase.getId()).isNotNull();
        assertThat(savedKnowledgeBase.getName()).isEqualTo("Full KnowledgeBase");
        assertThat(savedKnowledgeBase.getDescription()).isEqualTo("A comprehensive knowledge base");
        assertThat(savedKnowledgeBase.getMaxChunkSize()).isEqualTo(2048);
        assertThat(savedKnowledgeBase.getMinChunkSizeChars()).isEqualTo(256);
        assertThat(savedKnowledgeBase.getOverlap()).isEqualTo(128);
    }

    private KnowledgeBase createKnowledgeBase(String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName(name);

        return knowledgeBase;
    }
}
