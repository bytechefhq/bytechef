
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

package com.bytechef.dione.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.dione.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.category.domain.Category;
import com.bytechef.dione.configuration.domain.Integration;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.dione.configuration.dto.IntegrationDTO;
import com.bytechef.dione.configuration.repository.IntegrationRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
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

    @AfterEach
    @SuppressFBWarnings("NP")
    public void afterEach() {
        integrationRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testAddWorkflow() {
        Integration integration = new Integration();

        integration.setName("name");
        integration.setStatus(Integration.Status.UNPUBLISHED);

        integration = integrationRepository.save(integration);

        Workflow workflow = integrationFacade.addWorkflow(integration.getId(), "Workflow 1", "Description", null);

        assertThat(workflow.getDescription()).isEqualTo("Description");
        assertThat(workflow.getLabel()).isEqualTo("Workflow 1");
    }

    @Test
    public void testCreate() {
        Category category = categoryRepository.save(new Category("name"));

        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .category(category)
            .description("description")
            .name("name1")
            .status(Integration.Status.UNPUBLISHED)
            .tags(List.of(new Tag("tag1")))
            .build();

        integrationDTO = integrationFacade.create(integrationDTO);

        assertThat(integrationDTO.category()).isEqualTo(category);
        assertThat(integrationDTO.description()).isEqualTo("description");
        assertThat(integrationDTO.name()).isEqualTo("name1");
        assertThat(integrationDTO.id()).isNotNull();
        assertThat(integrationDTO.tags()).hasSize(1);
        assertThat(integrationDTO.workflowIds()).hasSize(0);
        assertThat(categoryRepository.count()).isEqualTo(1);
        assertThat(tagRepository.count()).isEqualTo(1);

        integrationDTO = IntegrationDTO.builder()
            .category(category)
            .description("description")
            .name("name2")
            .status(Integration.Status.UNPUBLISHED)
            .tags(List.of(new Tag("tag1")))
            .workflowIds(List.of("workflow2"))
            .build();

        integrationDTO = integrationFacade.create(integrationDTO);

        assertThat(integrationDTO.workflowIds()).hasSize(1);
        assertThat(integrationDTO.workflowIds()).contains("workflow2");
    }

    @Test
    public void testDelete() {
        IntegrationDTO integrationDTO1 = IntegrationDTO.builder()
            .name("name1")
            .status(Integration.Status.UNPUBLISHED)
            .tags(List.of(new Tag("tag1")))
            .build();

        integrationDTO1 = integrationFacade.create(integrationDTO1);

        IntegrationDTO integrationDTO2 = IntegrationDTO.builder()
            .name("name2")
            .status(Integration.Status.UNPUBLISHED)
            .tags(List.of(new Tag("tag1")))
            .build();

        integrationDTO2 = integrationFacade.create(integrationDTO2);

        assertThat(integrationRepository.count()).isEqualTo(2);
        assertThat(tagRepository.count()).isEqualTo(1);

        integrationFacade.delete(integrationDTO1.id());

        assertThat(integrationRepository.count()).isEqualTo(1);

        integrationFacade.delete(integrationDTO2.id());

        assertThat(integrationRepository.count()).isEqualTo(0);
        assertThat(tagRepository.count()).isEqualTo(1);
    }

    @Test
    public void testGetIntegration() {
        Integration integration = new Integration();

        Category category = categoryRepository.save(new Category("category1"));

        integration.setCategory(category);
        integration.setName("name");
        integration.setStatus(Integration.Status.UNPUBLISHED);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        integration.setTags(List.of(tag1, tag2));

        integration = integrationRepository.save(integration);

        assertThat(integrationFacade.getIntegration(integration.getId()))
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("id", integration.getId())
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("status", Integration.Status.UNPUBLISHED)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetIntegrations() {
        Integration integration = new Integration();

        Category category = categoryRepository.save(new Category("category1"));

        integration.setCategory(category);
        integration.setName("name");
        integration.setStatus(Integration.Status.UNPUBLISHED);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        integration.setTags(List.of(tag1, tag2));

        integration = integrationRepository.save(integration);

        List<IntegrationDTO> integrationDTOs = integrationFacade.getIntegrations(null, null);

        assertThat(CollectionUtils.map(integrationDTOs, IntegrationDTO::toIntegration)).isEqualTo(List.of(integration));

        IntegrationDTO integrationDTO = integrationDTOs.get(0);

        assertThat(integrationFacade.getIntegration(integrationDTO.id()))
            .isEqualTo(integrationDTO)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetIntegrationTags() {
        Integration integration = new Integration();

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        integration.setName("name");
        integration.setStatus(Integration.Status.UNPUBLISHED);
        integration.setTags(List.of(tag1, tag2));

        integrationRepository.save(integration);

        assertThat(integrationFacade.getIntegrationTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");

        integration = new Integration();

        integration.setName("name2");
        integration.setStatus(Integration.Status.UNPUBLISHED);

        tag1 = OptionalUtils.get(tagRepository.findById(tag1.getId()));

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
        Workflow workflow = new Workflow("{\"tasks\":[]}", Workflow.Format.JSON);

        workflow.setNew(true);

        workflow = workflowRepository.save(workflow);

        Integration integration = new Integration();

        integration.setName("name");
        integration.setStatus(Integration.Status.UNPUBLISHED);
        integration.setWorkflowIds(List.of(workflow.getId()));

        integration = integrationRepository.save(integration);

        List<Workflow> workflows = integrationFacade.getIntegrationWorkflows(integration.getId());

        assertThat(
            workflows.stream()
                .map(Workflow::getId)
                .toList())
                    .contains(workflow.getId());
    }

    @Test
    public void testUpdate() {
        Tag tag1 = new Tag("tag1");

        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .name("name")
            .status(Integration.Status.UNPUBLISHED)
            .tags(List.of(tag1, tagRepository.save(new Tag("tag2"))))
            .build();

        integrationDTO = integrationFacade.create(integrationDTO);

        assertThat(integrationDTO.tags()).hasSize(2);
        assertThat(integrationDTO.workflowIds()).hasSize(0);

        integrationDTO = IntegrationDTO.builder()
            .id(integrationDTO.id())
            .name("name")
            .status(Integration.Status.UNPUBLISHED)
            .tags(List.of(tag1))
            .build();

        integrationDTO = integrationFacade.update(integrationDTO);

        assertThat(integrationDTO.tags()).hasSize(1);
    }
}
