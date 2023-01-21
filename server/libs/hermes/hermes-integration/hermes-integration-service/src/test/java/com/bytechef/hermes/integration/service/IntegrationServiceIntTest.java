
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.integration.service;

import com.bytechef.hermes.integration.config.IntegrationIntTestConfiguration;
import com.bytechef.hermes.integration.domain.Category;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.repository.CategoryRepository;
import com.bytechef.hermes.integration.repository.IntegrationRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    @BeforeAll
    @SuppressFBWarnings("NP")
    public void beforeAll() {
        categoryRepository.deleteAll();

        category = categoryRepository.save(new Category("name"));
    }

    @BeforeEach
    @SuppressFBWarnings("NP")
    public void beforeEach() {
        integrationRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    public void testAddWorkflow() {
        Integration integration = integrationRepository.save(getIntegration());

        integration = integrationService.addWorkflow(integration.getId(), "workflow2");

        assertThat(integration.getWorkflowIds()).contains("workflow2");
    }

    @Test
    public void testCreate() {
        Integration integration = getIntegration();

        Tag tag = tagRepository.save(new Tag("tag1"));

        integration.setTags(List.of(tag));

        integration = integrationService.create(integration);

        assertThat(integration)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("description", "description")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tagIds", List.of(tag.getId()))
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow1"));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        Integration integration = integrationRepository.save(getIntegration());

        integrationService.delete(integration.getId());

        assertThat(integrationRepository.findById(integration.getId())).isNotPresent();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetIntegration() {
        Integration integration = integrationRepository.save(getIntegration());

        assertThat(integration).isEqualTo(integrationService.getIntegration(integration.getId()));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetIntegrations() {
        integrationRepository.save(getIntegration());

        assertThat(integrationService.getIntegrations(null, null)).hasSize(1);
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testUpdate() {
        Integration integration = integrationRepository.save(getIntegration());

        integration = integrationService.update(integration);

        assertThat(integration)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("description", "description")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tags", Collections.emptyList())
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow1"));

        Tag tag = tagRepository.save(new Tag("tag2"));

        integration.setDescription("description2");
        integration.setName("name2");
        integration.setTags(List.of(tag));
        integration.setWorkflowIds(List.of("workflow2"));

        Category category2 = categoryRepository.save(new Category("name2"));

        integration.setCategory(category2);

        integration = integrationService.update(integration);

        assertThat(integration)
            .hasFieldOrPropertyWithValue("category", category2)
            .hasFieldOrPropertyWithValue("description", "description2")
            .hasFieldOrPropertyWithValue("name", "name2")
            .hasFieldOrPropertyWithValue("tags", List.of(tag))
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow2"));
    }

    private Integration getIntegration() {
        Integration integration = new Integration();

        integration.addWorkflow("workflow1");
        integration.setCategory(category);
        integration.setDescription("description");
        integration.setName("name");

        return integration;
    }
}
