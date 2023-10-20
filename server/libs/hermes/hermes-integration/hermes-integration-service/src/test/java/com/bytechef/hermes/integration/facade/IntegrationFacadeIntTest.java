
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

package com.bytechef.hermes.integration.facade;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.WorkflowCrudRepository;
import com.bytechef.hermes.integration.config.IntegrationIntTestConfiguration;
import com.bytechef.category.domain.Category;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.hermes.integration.repository.IntegrationRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = "bytechef.workflow.workflow-repository.jdbc.enabled=true")
public class IntegrationFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private IntegrationFacade integrationFacade;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    private WorkflowCrudRepository workflowRepository;

    @BeforeEach
    @SuppressFBWarnings("NP")
    public void beforeEach() {
        integrationRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testAddWorkflow() {
        Integration integration = integrationRepository.save(new Integration("name"));

        integration = integrationFacade.addWorkflow(integration.getId(), "Workflow 1", "Description", null);

        List<String> workflowIds = integration.getWorkflowIds();

        Workflow workflow = workflowRepository.findById(workflowIds.iterator()
            .next())
            .orElseThrow();

        assertThat(workflow.getDescription()).isEqualTo("Description");
        assertThat(workflow.getLabel()).isEqualTo("Workflow 1");
    }

    @Test
    public void testCreate() {
        Integration integration = new Integration();

        integration.setName("name1");
        integration.setDescription("description");

        Category category = categoryRepository.save(new Category("name"));

        integration.setCategory(category);
        integration.setTags(List.of(new Tag("tag1")));

        integration = integrationFacade.create(integration);

        assertThat(integration.getCategoryId()).isEqualTo(category.getId());
        assertThat(integration.getDescription()).isEqualTo("description");
        assertThat(integration.getName()).isEqualTo("name1");
        assertThat(integration.getId()).isNotNull();
        assertThat(integration.getTagIds()).hasSize(1);
        assertThat(integration.getWorkflowIds()).hasSize(1);

        integration = new Integration();

        integration.setName("name2");
        integration.setWorkflowIds(List.of("workflow2"));

        integration = integrationFacade.create(integration);

        assertThat(integration.getWorkflowIds()).hasSize(1);
        assertThat(integration.getWorkflowIds()).contains("workflow2");
    }

    @Test
    public void testDelete() {
        Integration integration1 = new Integration();

        integration1.setName("name1");
        integration1.setTags(List.of(new Tag("tag1")));

        integration1 = integrationFacade.create(integration1);

        Integration integration2 = new Integration();

        integration2.setName("name2");
        integration2.setTags(List.of(new Tag("tag1")));

        integration2 = integrationFacade.create(integration2);

        assertThat(integrationRepository.count()).isEqualTo(2);
        assertThat(tagRepository.count()).isEqualTo(1);

        integrationFacade.delete(integration1.getId());

        assertThat(integrationRepository.count()).isEqualTo(1);

        integrationFacade.delete(integration2.getId());

        assertThat(integrationRepository.count()).isEqualTo(0);
        assertThat(tagRepository.count()).isEqualTo(1);
    }

    @Test
    public void testGetIntegration() {
        Integration integration = new Integration();

        Category category = categoryRepository.save(new Category("category1"));

        integration.setCategory(category);
        integration.setName("name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        integration.setTags(List.of(tag1, tag2));

        integration = integrationRepository.save(integration);

        assertThat(integrationFacade.getIntegration(integration.getId()))
            .isEqualTo(integration)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetIntegrations() {
        Integration integration = new Integration();

        Category category = categoryRepository.save(new Category("category1"));

        integration.setCategory(category);
        integration.setName("name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        integration.setTags(List.of(tag1, tag2));

        integration = integrationRepository.save(integration);

        List<Integration> integrations = integrationFacade.getIntegrations(null, null);

        assertThat(integrations).isEqualTo(List.of(integration));

        integration = integrations.get(0);

        assertThat(integrationFacade.getIntegration(integration.getId()))
            .isEqualTo(integration)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetIntegrationTags() {
        Integration integration = new Integration();

        Tag tag1 = tagRepository.save(new Tag("tag1"));

        integration.setName("name");
        integration.setTags(List.of(tag1, tagRepository.save(new Tag("tag2"))));

        integrationRepository.save(integration);

        assertThat(integrationFacade.getIntegrationTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");

        integration = new Integration();

        integration.setName("name2");

        tag1 = tagRepository.findById(tag1.getId())
            .orElseThrow();

        integration.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        integrationRepository.save(integration);

        assertThat(integrationFacade.getIntegrationTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2", "tag3");

        integrationRepository.deleteById(integration.getId());

        assertThat(integrationFacade.getIntegrationTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetIntegrationWorkflows() {
        Workflow workflow = new Workflow("{}", Workflow.Format.JSON);

        workflow.setNew(true);

        workflow = workflowRepository.save(workflow);

        Integration integration = new Integration();

        integration.setName("name");

        integration.setWorkflowIds(List.of(workflow.getId()));

        integration = integrationRepository.save(integration);

        List<Workflow> workflows = integrationFacade.getIntegrationWorkflows(integration.getId());

        assertThat(workflows).contains(workflow);
    }

    @Test
    public void testUpdate() {
        Integration integration = new Integration();

        integration.setName("name");

        Tag tag1 = new Tag("tag1");

        integration.setTags(List.of(tag1, tagRepository.save(new Tag("tag2"))));

        integration = integrationFacade.create(integration);

        assertThat(integration.getTagIds()).hasSize(2);
        assertThat(integration.getWorkflowIds()).hasSize(1);

        integration.setTags(List.of(tag1));

        integrationRepository.save(integration);

        integration = integrationFacade.update(integration);

        assertThat(integration.getTagIds()).hasSize(1);
    }
}
