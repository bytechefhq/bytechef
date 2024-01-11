/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.category.domain.Category;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class IntegrationServiceIntTest {

    private Category category;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    public void beforeEach() {
        categoryRepository.deleteAll();

        category = categoryRepository.save(new Category("name"));
    }

    @AfterEach
    public void afterEach() {
        integrationRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    public void testAddWorkflow() {
        Integration integration = integrationRepository.save(getIntegration());

        integration = integrationService.addWorkflow(Validate.notNull(integration.getId(), "id"), "workflow2");

        assertThat(integration.getWorkflowIds()).contains("workflow2");
    }

    @Test
    public void testCreate() {
        Integration integration = getIntegration();

        Tag tag = tagRepository.save(new Tag("tag1"));

        integration.setTags(List.of(tag));

        integration = integrationService.create(integration);

        assertThat(integration)
            .hasFieldOrPropertyWithValue("categoryId", category.getId())
            .hasFieldOrPropertyWithValue("tagIds", List.of(Validate.notNull(tag.getId(), "id")))
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow1"));
    }

    @Test
    public void testDelete() {
        Integration integration = integrationRepository.save(getIntegration());

        integrationService.delete(Validate.notNull(integration.getId(), "id"));

        assertThat(integrationRepository.findById(integration.getId())).isNotPresent();
    }

    @Test
    public void testGetIntegration() {
        Integration integration = integrationRepository.save(getIntegration());

        assertThat(integration).isEqualTo(
            integrationService.getIntegration(Validate.notNull(integration.getId(), "id")));
    }

    @Test
    public void testGetIntegrations() {
        Integration integration = integrationRepository.save(getIntegration());

        assertThat(integrationService.getIntegrations(null, null, null, null)).hasSize(1);

        Category category = new Category("category1");

        category = categoryRepository.save(category);

        integration.setCategory(category);

        integration = integrationRepository.save(integration);

        assertThat(integrationService.getIntegrations(category.getId(), null, null, null)).hasSize(1);

        assertThat(integrationService.getIntegrations(Long.MAX_VALUE, null, null, null)).hasSize(0);

        Tag tag = new Tag("tag1");

        tag = tagRepository.save(tag);

        integration.setTags(List.of(tag));

        integrationRepository.save(integration);

        assertThat(integrationService.getIntegrations(null, null, tag.getId(), false)).hasSize(1);
        assertThat(integrationService.getIntegrations(null, null, Long.MAX_VALUE, false)).hasSize(0);
        assertThat(integrationService.getIntegrations(Long.MAX_VALUE, null, Long.MAX_VALUE, false)).hasSize(0);
    }

    @Test
    public void testUpdate() {
        Integration integration = integrationRepository.save(getIntegration());

        Tag tag = tagRepository.save(new Tag("tag2"));

        integration.setTags(List.of(tag));
        integration.setWorkflowIds(List.of("workflow2"));

        Category category2 = categoryRepository.save(new Category("name2"));

        integration.setCategory(category2);
        integration.setTagIds(List.of(Validate.notNull(tag.getId(), "id")));
        integration.setWorkflowIds(List.of("workflow2"));

        integration = integrationService.update(integration);

        assertThat(integration)
            .hasFieldOrPropertyWithValue("categoryId", category2.getId())
            .hasFieldOrPropertyWithValue("tagIds", List.of(tag.getId()))
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow2"));
    }

    private Integration getIntegration() {
        return Integration.builder()
            .categoryId(category.getId())
            .componentName("componentName")
            .integrationVersion(1)
            .status(Integration.Status.UNPUBLISHED)
            .workflowIds(List.of("workflow1"))
            .build();
    }
}
