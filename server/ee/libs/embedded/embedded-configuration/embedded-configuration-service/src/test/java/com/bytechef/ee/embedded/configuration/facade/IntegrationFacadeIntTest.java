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

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
public class IntegrationFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private IntegrationFacade integrationFacade;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationTagFacade integrationTagFacade;

    @Autowired
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    private WorkflowFacade workflowFacade;

    @Autowired
    private WorkflowCrudRepository workflowRepository;

    @BeforeEach
    void beforeEach() {
        Mockito.when(componentDefinitionService.getComponentDefinition(Mockito.anyString(), Mockito.any()))
            .thenReturn(new ComponentDefinition("componentName"));
    }

    @AfterEach
    public void afterEach() {
        integrationRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Disabled
    @Test
    public void testAddWorkflow() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");

        integration = integrationRepository.save(integration);

        // TODO remove
        Mockito.when(workflowFacade.getWorkflow(Mockito.anyString()))
            .thenReturn(new com.bytechef.platform.configuration.dto.WorkflowDTO(
                new Workflow(
                    "{\"label\": \"New Workflow\", \"description\": \"Description\", \"tasks\": []}",
                    Workflow.Format.JSON),
                List.of(), List.of()));

        IntegrationWorkflowDTO workflow = integrationWorkflowFacade.getIntegrationWorkflow(
            integrationWorkflowFacade.addWorkflow(
                Validate.notNull(integration.getId(), "id"),
                "{\"label\": \"New Workflow\", \"description\": \"Description\", \"tasks\": []}"));

        assertThat(workflow.getDescription()).isEqualTo("Description");
        assertThat(workflow.getLabel()).isEqualTo("New Workflow");
    }

    @Test
    public void testCreateIntegration() {
        Category category = categoryRepository.save(new Category("name"));

        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .category(category)
            .componentName("componentName")
            .name("Name")
            .tags(List.of(new Tag("tag1")))
            .build();

        integrationDTO = integrationFacade.getIntegration(integrationFacade.createIntegration(integrationDTO));

        assertThat(integrationDTO.category()).isEqualTo(category);
        assertThat(integrationDTO.id()).isNotNull();
        assertThat(integrationDTO.tags()).hasSize(1);
        assertThat(integrationDTO.integrationWorkflowIds()).hasSize(0);
        assertThat(categoryRepository.count()).isEqualTo(1);
        assertThat(tagRepository.count()).isEqualTo(1);
    }

    @Test
    public void testDelete() {
        IntegrationDTO integrationDTO1 = IntegrationDTO.builder()
            .componentName("componentName1")
            .name("Name")
            .tags(List.of(new Tag("tag1")))
            .build();

        integrationDTO1 = integrationFacade.getIntegration(integrationFacade.createIntegration(integrationDTO1));

        IntegrationDTO integrationDTO2 = IntegrationDTO.builder()
            .componentName("componentName2")
            .name("Name")
            .tags(List.of(new Tag("tag1")))
            .build();

        integrationDTO2 = integrationFacade.getIntegration(integrationFacade.createIntegration(integrationDTO2));

        assertThat(integrationRepository.count()).isEqualTo(2);
        assertThat(tagRepository.count()).isEqualTo(1);

        integrationFacade.deleteIntegration(integrationDTO1.id());

        assertThat(integrationRepository.count()).isEqualTo(1);

        integrationFacade.deleteIntegration(integrationDTO2.id());

        assertThat(integrationRepository.count()).isEqualTo(0);
        assertThat(tagRepository.count()).isEqualTo(1);
    }

    @Test
    public void testGetIntegration() {
        Integration integration = new Integration();

        Category category = categoryRepository.save(new Category("category1"));

        integration.setCategory(category);
        integration.setComponentName("componentName");
        integration.setName("Name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        integration.setTags(List.of(tag1, tag2));

        integration = integrationRepository.save(integration);

        assertThat(integrationFacade.getIntegration(Validate.notNull(integration.getId(), "id")))
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("id", integration.getId())
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetIntegrations() {
        Integration integration = new Integration();

        Category category = categoryRepository.save(new Category("category1"));

        integration.setCategory(category);
        integration.setComponentName("componentName");
        integration.setName("Name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        integration.setTags(List.of(tag1, tag2));

        integration = integrationRepository.save(integration);

        List<IntegrationDTO> integrationDTOs = integrationFacade.getIntegrations(null, false, null, null, true);

        assertThat(CollectionUtils.map(integrationDTOs, IntegrationDTO::toIntegration))
            .isEqualTo(List.of(integration));

        IntegrationDTO integrationDTO = integrationDTOs.getFirst();

        assertThat(integrationFacade.getIntegration(integrationDTO.id()))
            .isEqualTo(integrationDTO)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetIntegrationTags() {
        Integration integration = new Integration();

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        integration.setComponentName("componentName");
        integration.setName("Name");
        integration.setTags(List.of(tag1, tag2));

        integrationRepository.save(integration);

        assertThat(
            integrationTagFacade.getIntegrationTags()
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet())).contains("tag1", "tag2");

        integration = new Integration();

        integration.setComponentName("componentName2");
        integration.setName("Name");

        tag1 = OptionalUtils.get(tagRepository.findById(Validate.notNull(tag1.getId(), "id")));

        integration.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        integrationRepository.save(integration);

        assertThat(
            integrationTagFacade.getIntegrationTags()
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet())).contains("tag1", "tag2", "tag3");

        integrationRepository.deleteById(Validate.notNull(integration.getId(), "id"));

        assertThat(
            integrationTagFacade.getIntegrationTags()
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet())).contains("tag1", "tag2");
    }

    @Disabled
    @Test
    public void testGetIntegrationWorkflows() {
        Workflow workflow = new Workflow("{\"tasks\":[]}", Workflow.Format.JSON);

        workflow.setNew(true);

        workflow = workflowRepository.save(workflow);

        Integration integration = new Integration();

        integration.setComponentName("componentName");

        integration = integrationRepository.save(integration);

        integrationWorkflowRepository.save(
            new IntegrationWorkflow(
                integration.getId(), Validate.notNull(integration.getLastIntegrationVersion(), "lastVersion"),
                Validate.notNull(workflow.getId(), "id"), UUID.randomUUID()));

        // TODO remove
        Mockito.when(workflowFacade.getWorkflow(Mockito.anyString()))
            .thenReturn(new com.bytechef.platform.configuration.dto.WorkflowDTO(workflow, List.of(), List.of()));

        List<IntegrationWorkflowDTO> workflows = integrationWorkflowFacade.getIntegrationWorkflows(
            Validate.notNull(integration.getId(), "id"));

        assertThat(
            workflows.stream()
                .map(IntegrationWorkflowDTO::getId)
                .toList())
                    .contains(workflow.getId());
    }

    @Test
    public void testUpdateIntegrationTagsIntegration() {
        Tag tag1 = new Tag("tag1");

        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .componentName("componentName")
            .name("Name")
            .tags(List.of(tag1, tagRepository.save(new Tag("tag2"))))
            .build();

        integrationDTO = integrationFacade.getIntegration(integrationFacade.createIntegration(integrationDTO));

        assertThat(integrationDTO.tags()).hasSize(2);
        assertThat(integrationDTO.integrationWorkflowIds()).hasSize(0);

        integrationDTO = IntegrationDTO.builder()
            .componentName("componentName")
            .id(integrationDTO.id())
            .name("Name")
            .tags(List.of(tag1))
            .version(integrationDTO.version())
            .build();

        integrationFacade.updateIntegration(integrationDTO);

        integrationDTO = integrationFacade.getIntegration(integrationDTO.id());

        assertThat(integrationDTO.tags()).hasSize(1);
    }

    @TestConfiguration
    public static class IntegrationFacadeIntTestConfiguration {

        @Bean
        WorkflowService workflowService(
            CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
            List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
        }
    }
}
