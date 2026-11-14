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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRows;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, IntegrationFacadeIntTest.IntegrationFacadeIntTestConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, ClusterElementDefinitionService.class,
    ComponentConnectionFacade.class, ComponentDefinitionFacade.class, ComponentDefinitionService.class,
    ConnectedUserService.class, ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class, JobFacade.class, JobService.class,
    McpComponentService.class, McpIntegrationComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpIntegrationInstanceWorkflowService.class, McpIntegrationInstanceRows.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class, ProjectService.class,
    ProjectWorkflowFacade.class, ProjectWorkflowService.class, TaskDispatcherDefinitionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, WebhookTriggerTestFacade.class, WorkflowCacheManager.class, WorkflowFacade.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class,
    WorkflowTestConfigurationService.class
})
public class IntegrationFacadeIntTest {

    private static final String ADDED_WORKFLOW_DEFINITION =
        "{\"label\": \"Added\", \"description\": \"Test Description\", \"tasks\": []}";

    private static final String VERSION_1_WORKFLOW_DEFINITION =
        "{\"label\": \"Version 1\", \"description\": \"Test Description\", \"tasks\": []}";

    private static final String VERSION_2_WORKFLOW_DEFINITION =
        "{\"label\": \"Version 2\", \"description\": \"Test Description\", \"tasks\": []}";

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private IntegrationFacade integrationFacade;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        Mockito.when(componentDefinitionService.getComponentDefinition(Mockito.anyString(), Mockito.any()))
            .thenReturn(new ComponentDefinition("componentName"));
    }

    @AfterEach
    public void afterEach() {
        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();

        for (Workflow workflow : workflowRepository.findAll()) {
            workflowRepository.deleteById(workflow.getId());
        }

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
    public void testDeleteOfAnUnknownIntegrationFails() {
        assertThatThrownBy(() -> integrationFacade.deleteIntegration(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Integration " + Long.MAX_VALUE + " not found");
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
    public void testDeleteCodeIntegration() {
        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .componentName("googleMail")
            .name("Gmail")
            .build();

        long integrationId = integrationFacade.createIntegration(integrationDTO);

        Long codeWorkflowContainerId = jdbcTemplate.queryForObject(
            """
                INSERT INTO code_workflow_container (name, uuid, external_version, language, workflows, created_date,
                    created_by, last_modified_date, last_modified_by, version)
                VALUES ('googleMail', gen_random_uuid(), '1.0.0', 0, '', now(), 'admin', now(), 'admin', 0)
                RETURNING id
                """,
            Long.class);

        jdbcTemplate.update(
            """
                INSERT INTO integration_code_workflow (code_workflow_container_id, integration_id,
                    integration_version, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, 1, now(), 'admin', now(), 'admin', 0)
                """,
            codeWorkflowContainerId, integrationId);

        integrationFacade.deleteIntegration(integrationId);

        assertThat(integrationRepository.findById(integrationId)).isEmpty();
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM integration_code_workflow WHERE integration_id = ?", Long.class, integrationId))
                    .isZero();
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
            .usingRecursiveComparison()
            .ignoringFields("developmentConfigurationCount")
            .isEqualTo(integrationDTO);
        assertThat(integrationFacade.getIntegration(integrationDTO.id()))
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetIntegrationsDevelopmentConfigurationCount() {
        Integration developmentIntegration = createIntegration("development");

        createIntegrationInstanceConfiguration(developmentIntegration.getId(), Environment.DEVELOPMENT);
        createIntegrationInstanceConfiguration(developmentIntegration.getId(), Environment.DEVELOPMENT);

        Integration productionIntegration = createIntegration("production");

        createIntegrationInstanceConfiguration(productionIntegration.getId(), Environment.PRODUCTION);

        List<IntegrationDTO> integrationDTOs = integrationFacade.getIntegrations(null, false, null, null, true);

        assertThat(integrationDTOs)
            .filteredOn(integrationDTO -> integrationDTO.id()
                .equals(developmentIntegration.getId()))
            .singleElement()
            .extracting(IntegrationDTO::developmentConfigurationCount)
            .isEqualTo(2);
        assertThat(integrationDTOs)
            .filteredOn(integrationDTO -> integrationDTO.id()
                .equals(productionIntegration.getId()))
            .singleElement()
            .extracting(IntegrationDTO::developmentConfigurationCount)
            .isEqualTo(0);

        IntegrationDTO developmentIntegrationDTO = integrationFacade.getIntegration(developmentIntegration.getId());

        assertThat(developmentIntegrationDTO.developmentConfigurationCount()).isNull();
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
    public void testRestoreIntegrationVersion() {
        long integrationId = Validate.notNull(createIntegration("restore").getId(), "id");

        integrationWorkflowFacade.addWorkflow(integrationId, VERSION_1_WORKFLOW_DEFINITION);

        integrationFacade.publishIntegration(integrationId, "first");

        IntegrationWorkflow publishedIntegrationWorkflow = integrationWorkflowRepository
            .findAllByIntegrationIdAndIntegrationVersion(integrationId, 1)
            .getFirst();
        IntegrationWorkflow draftIntegrationWorkflow = integrationWorkflowRepository
            .findAllByIntegrationIdAndIntegrationVersion(integrationId, 2)
            .getFirst();

        String draftWorkflowId = draftIntegrationWorkflow.getWorkflowId();

        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(draftWorkflowId, VERSION_2_WORKFLOW_DEFINITION, draftWorkflow.getVersion());

        IntegrationWorkflow addedIntegrationWorkflow = integrationWorkflowRepository
            .findById(integrationWorkflowFacade.addWorkflow(integrationId, ADDED_WORKFLOW_DEFINITION))
            .orElseThrow();

        assertThat(integrationWorkflowRepository.findAllByIntegrationIdAndIntegrationVersion(integrationId, 2))
            .hasSize(2);

        integrationFacade.restoreIntegrationVersion(integrationId, 1);

        List<IntegrationWorkflow> restoredIntegrationWorkflows = integrationWorkflowRepository
            .findAllByIntegrationIdAndIntegrationVersion(integrationId, 2);

        assertThat(restoredIntegrationWorkflows).hasSize(1);

        IntegrationWorkflow restoredIntegrationWorkflow = restoredIntegrationWorkflows.getFirst();

        assertThat(restoredIntegrationWorkflow.getId()).isEqualTo(draftIntegrationWorkflow.getId());
        assertThat(restoredIntegrationWorkflow.getUuid()).isEqualTo(publishedIntegrationWorkflow.getUuid());
        assertThat(restoredIntegrationWorkflow.getWorkflowId())
            .isNotEqualTo(publishedIntegrationWorkflow.getWorkflowId())
            .isNotEqualTo(draftWorkflowId);

        Workflow restoredWorkflow = workflowRepository.findById(restoredIntegrationWorkflow.getWorkflowId())
            .orElseThrow();

        assertThat(restoredWorkflow.getLabel()).isEqualTo("Version 1");

        assertThat(workflowRepository.findById(draftWorkflowId)).isEmpty();
        assertThat(workflowRepository.findById(addedIntegrationWorkflow.getWorkflowId())).isEmpty();
        assertThat(workflowRepository.findById(publishedIntegrationWorkflow.getWorkflowId())).isPresent();
        assertThat(integrationWorkflowRepository.findAllByIntegrationIdAndIntegrationVersion(integrationId, 1))
            .extracting(IntegrationWorkflow::getWorkflowId)
            .containsExactly(publishedIntegrationWorkflow.getWorkflowId());
    }

    @Test
    public void testRestoreIntegrationVersionAddsWorkflowsMissingFromDraft() {
        long integrationId = Validate.notNull(createIntegration("restore-missing").getId(), "id");

        integrationWorkflowFacade.addWorkflow(integrationId, VERSION_1_WORKFLOW_DEFINITION);

        integrationFacade.publishIntegration(integrationId, "first");

        IntegrationWorkflow publishedIntegrationWorkflow = integrationWorkflowRepository
            .findAllByIntegrationIdAndIntegrationVersion(integrationId, 1)
            .getFirst();
        IntegrationWorkflow draftIntegrationWorkflow = integrationWorkflowRepository
            .findAllByIntegrationIdAndIntegrationVersion(integrationId, 2)
            .getFirst();

        integrationWorkflowRepository.deleteById(Validate.notNull(draftIntegrationWorkflow.getId(), "id"));
        workflowRepository.deleteById(draftIntegrationWorkflow.getWorkflowId());

        assertThat(integrationWorkflowRepository.findAllByIntegrationIdAndIntegrationVersion(integrationId, 2))
            .isEmpty();

        integrationFacade.restoreIntegrationVersion(integrationId, 1);

        List<IntegrationWorkflow> restoredIntegrationWorkflows = integrationWorkflowRepository
            .findAllByIntegrationIdAndIntegrationVersion(integrationId, 2);

        assertThat(restoredIntegrationWorkflows).hasSize(1);

        IntegrationWorkflow restoredIntegrationWorkflow = restoredIntegrationWorkflows.getFirst();

        assertThat(restoredIntegrationWorkflow.getUuid()).isEqualTo(publishedIntegrationWorkflow.getUuid());
        assertThat(restoredIntegrationWorkflow.getWorkflowId())
            .isNotEqualTo(publishedIntegrationWorkflow.getWorkflowId());

        Workflow restoredWorkflow = workflowRepository.findById(restoredIntegrationWorkflow.getWorkflowId())
            .orElseThrow();

        assertThat(restoredWorkflow.getLabel()).isEqualTo("Version 1");
    }

    @Test
    public void testRestoreIntegrationVersionRejectsDraftVersion() {
        long integrationId = Validate.notNull(createIntegration("restore-draft").getId(), "id");

        integrationWorkflowFacade.addWorkflow(integrationId, VERSION_1_WORKFLOW_DEFINITION);

        assertThatThrownBy(() -> integrationFacade.restoreIntegrationVersion(integrationId, 1))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("is not published");
        assertThatThrownBy(() -> integrationFacade.restoreIntegrationVersion(integrationId, 5))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("not found");
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

    private Integration createIntegration(String name) {
        Integration integration = new Integration();

        integration.setComponentName("componentName-" + name);
        integration.setName(name);

        return integrationRepository.save(integration);
    }

    private void createIntegrationInstanceConfiguration(long integrationId, Environment environment) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfigurationRepository.save(integrationInstanceConfiguration);
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
