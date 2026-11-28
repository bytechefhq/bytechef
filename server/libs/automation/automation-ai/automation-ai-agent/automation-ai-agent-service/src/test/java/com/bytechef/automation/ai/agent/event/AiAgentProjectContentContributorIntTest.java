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

package com.bytechef.automation.ai.agent.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.agent.config.AutomationAiAgentIntTestConfiguration;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.agent.repository.AiAgentChannelRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentElementRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentVersionRepository;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflowType;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.listener.ProjectContentContributor;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationAiAgentIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class AiAgentProjectContentContributorIntTest {

    private static final String SLACK_CHANNEL_TYPE = "slack";

    @Autowired
    private AiAgentFacade agentFacade;

    @Autowired
    private AiAgentService agentService;

    @Autowired
    private AiAgentChannelRepository agentChannelRepository;

    @Autowired
    private AiAgentElementRepository agentElementRepository;

    @Autowired
    private AiAgentRepository agentRepository;

    @Autowired
    private AiAgentVersionRepository agentVersionRepository;

    @Autowired
    private List<ProjectContentContributor> projectContentContributors;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private ProjectContentContributor agentContentContributor;
    private Long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        workspaceId = workspace.getId();

        agentContentContributor = projectContentContributors.stream()
            .filter(contributor -> contributor instanceof AiAgentProjectContentContributor)
            .findFirst()
            .orElseThrow();
    }

    @AfterEach
    void afterEach() {
        agentVersionRepository.deleteAll();
        agentElementRepository.deleteAll();
        agentChannelRepository.deleteAll();
        agentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }

        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testDuplicateCopiesEveryAgentWithElementsChannelsAndOwnWorkflows() {
        AiAgent supportAgent = createSupportAgent();
        long sourceProjectId = agentService.getProjectId(supportAgent);

        AiAgent salesAgent = agentFacade.createAgent("Sales Bot", "Sells", workspaceId, sourceProjectId)
            .agent();

        agentFacade.addAgentElement(
            supportAgent.getId(), AiAgentElement.KIND_SUB_AGENT, salesAgent.getId(), Map.of(), null);

        long duplicateProjectId = createProject("Support Bot (copy)");

        agentContentContributor.onProjectDuplicated(sourceProjectId, duplicateProjectId);

        List<AiAgent> copiedAgents = agentService.getProjectAgents(duplicateProjectId);

        assertThat(copiedAgents).extracting(AiAgent::getTitle)
            .containsExactlyInAnyOrder("Support Bot", "Sales Bot");
        assertThat(copiedAgents).extracting(AiAgent::getName)
            .doesNotContain(supportAgent.getName(), salesAgent.getName());

        AiAgent copiedSupportAgent = findByTitle(copiedAgents, "Support Bot");
        AiAgent copiedSalesAgent = findByTitle(copiedAgents, "Sales Bot");

        assertThat(copiedSupportAgent.getInstructions()).isEqualTo("Be concise.");
        assertThat(copiedSupportAgent.getSettings()
            .get("temperature")).isEqualTo(0.2);

        AiAgentDTO copiedSupportAgentDTO = agentFacade.getAgent(copiedSupportAgent.getId());

        assertThat(copiedSupportAgentDTO.channels())
            .filteredOn(channel -> SLACK_CHANNEL_TYPE.equals(channel.getChannelType()))
            .singleElement()
            .satisfies(channel -> {
                assertThat(channel.getConnectionId()).isEqualTo(77L);
                assertThat(channel.getParameters()
                    .get("channel")).isEqualTo("#support");
            });

        Map<String, AiAgentElement> elementsByKind = copiedSupportAgentDTO.elements()
            .stream()
            .collect(Collectors.toMap(AiAgentElement::getKind, element -> element));

        assertThat(elementsByKind).containsOnlyKeys(
            AiAgentElement.KIND_CHAT_MEMORY, AiAgentElement.KIND_MODEL, AiAgentElement.KIND_SKILL,
            AiAgentElement.KIND_SUB_AGENT);
        assertThat(elementsByKind.get(AiAgentElement.KIND_MODEL)
            .getConnectionId()).isEqualTo(42L);
        assertThat(elementsByKind.get(AiAgentElement.KIND_SKILL)
            .getReferenceId()).isEqualTo(12345L);
        assertThat(elementsByKind.get(AiAgentElement.KIND_SUB_AGENT)
            .getReferenceId())
                .as("a sub-agent inside the duplicated project points at that agent's copy")
                .isEqualTo(copiedSalesAgent.getId());

        List<String> sourceAgentWorkflowIds = agentWorkflowIds(sourceProjectId);
        List<String> copiedAgentWorkflowIds = agentWorkflowIds(duplicateProjectId);

        assertThat(sourceAgentWorkflowIds).hasSize(2);
        assertThat(copiedAgentWorkflowIds).hasSize(2)
            .doesNotContainAnyElementsOf(sourceAgentWorkflowIds);
        assertThat(agentService.getProjectAgents(sourceProjectId)).hasSize(2);
    }

    @Test
    void testExportThenImportRecreatesTheAgentsInTheNewProject() {
        AiAgent supportAgent = createSupportAgent();
        long sourceProjectId = agentService.getProjectId(supportAgent);

        Map<String, byte[]> files = agentContentContributor.exportProjectContent(
            sourceProjectId, projectService.getProject(sourceProjectId)
                .getLastProjectVersion());

        assertThat(files).containsOnlyKeys("agents/" + supportAgent.getName() + ".json");

        long importedProjectId = createProject("Imported");

        agentContentContributor.importProjectContent(importedProjectId, workspaceId, files);

        List<AiAgent> importedAgents = agentService.getProjectAgents(importedProjectId);

        assertThat(importedAgents).singleElement()
            .satisfies(agent -> {
                assertThat(agent.getTitle()).isEqualTo("Support Bot");
                assertThat(agent.getInstructions()).isEqualTo("Be concise.");
            });

        AiAgentDTO importedAgentDTO = agentFacade.getAgent(importedAgents.getFirst()
            .getId());

        assertThat(importedAgentDTO.channels()).extracting(AiAgentChannel::getChannelType)
            .contains(SLACK_CHANNEL_TYPE);
        assertThat(importedAgentDTO.elements()).extracting(AiAgentElement::getKind)
            .containsExactlyInAnyOrder(AiAgentElement.KIND_CHAT_MEMORY, AiAgentElement.KIND_MODEL);
    }

    @Test
    void testExportOfAPublishedVersionContainsTheAgentAsItWasPublished() {
        AiAgent supportAgent = createSupportAgent();
        long projectId = agentService.getProjectId(supportAgent);

        int publishedVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        int draftVersion = publishProject(projectId);

        agentFacade.updateAgent(supportAgent.getId(), null, null, "Be thorough.");

        AiAgent laterAgent = agentFacade.createAgent("Later Bot", "Added after publish", workspaceId, projectId)
            .agent();

        Map<String, byte[]> publishedFiles = agentContentContributor.exportProjectContent(
            projectId, publishedVersion);

        assertThat(publishedFiles).containsOnlyKeys("agents/" + supportAgent.getName() + ".json");
        assertThat(new String(publishedFiles.get("agents/" + supportAgent.getName() + ".json"), StandardCharsets.UTF_8))
            .as("an exported version carries no connection or reference ids, like the draft export")
            .doesNotContain("connectionId", "referenceId", "uuid");
        assertThat(JsonUtils.readMap(new String(
            publishedFiles.get("agents/" + supportAgent.getName() + ".json"), StandardCharsets.UTF_8))
            .get("instructions")).isEqualTo("Be concise.");

        Map<String, byte[]> draftFiles = agentContentContributor.exportProjectContent(projectId, draftVersion);

        assertThat(draftFiles).containsOnlyKeys(
            "agents/" + supportAgent.getName() + ".json", "agents/" + laterAgent.getName() + ".json");
        assertThat(JsonUtils.readMap(new String(
            draftFiles.get("agents/" + supportAgent.getName() + ".json"), StandardCharsets.UTF_8))
            .get("instructions")).isEqualTo("Be thorough.");
    }

    @Test
    void testRestoreBringsBackTheAgentsAsTheyWerePublished() {
        AiAgent supportAgent = createSupportAgent();
        long projectId = agentService.getProjectId(supportAgent);

        AiAgent salesAgent = agentFacade.createAgent("Sales Bot", "Sells", workspaceId, projectId)
            .agent();

        agentFacade.addAgentElement(
            salesAgent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        publishProject(projectId);

        agentFacade.addAgentElement(
            supportAgent.getId(), AiAgentElement.KIND_SUB_AGENT, salesAgent.getId(), Map.of(), null);

        int publishedVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        publishProject(projectId);

        AiAgentDTO supportAgentDTO = agentFacade.getAgent(supportAgent.getId());

        agentFacade.updateAgent(supportAgent.getId(), null, null, "Be thorough.");
        agentFacade.updateAgentChannel(
            findChannel(supportAgentDTO, SLACK_CHANNEL_TYPE).getId(), Map.of("channel", "#sales"), 55L);
        agentFacade.deleteAgentElement(findElement(supportAgentDTO, AiAgentElement.KIND_SUB_AGENT).getId());
        agentFacade.deleteAgent(salesAgent.getId());
        agentFacade.createAgent("Later Bot", "Added after publish", workspaceId, projectId);

        agentContentContributor.restoreProjectContent(projectId, publishedVersion);

        List<AiAgent> restoredAgents = agentService.getProjectAgents(projectId);

        assertThat(restoredAgents).extracting(AiAgent::getTitle)
            .containsExactlyInAnyOrder("Support Bot", "Sales Bot");

        AiAgent restoredSalesAgent = findByTitle(restoredAgents, "Sales Bot");

        assertThat(restoredSalesAgent.getUuid()).isEqualTo(salesAgent.getUuid());
        assertThat(restoredSalesAgent.getProjectWorkflowUuid()).isEqualTo(salesAgent.getProjectWorkflowUuid());

        AiAgentDTO restoredSupportAgentDTO = agentFacade.getAgent(supportAgent.getId());

        assertThat(restoredSupportAgentDTO.agent()
            .getInstructions()).isEqualTo("Be concise.");
        assertThat(findChannel(restoredSupportAgentDTO, SLACK_CHANNEL_TYPE)).satisfies(channel -> {
            assertThat(channel.getConnectionId()).isEqualTo(77L);
            assertThat(channel.getParameters()
                .get("channel")).isEqualTo("#support");
        });
        assertThat(findElement(restoredSupportAgentDTO, AiAgentElement.KIND_MODEL)
            .getConnectionId()).isEqualTo(42L);
        assertThat(findElement(restoredSupportAgentDTO, AiAgentElement.KIND_SKILL)
            .getReferenceId()).isEqualTo(12345L);
        assertThat(findElement(restoredSupportAgentDTO, AiAgentElement.KIND_SUB_AGENT)
            .getReferenceId()).isEqualTo(restoredSalesAgent.getId());

        Workflow supportWorkflow = workflowService.getWorkflow(agentFacade.getDraftWorkflowId(supportAgent.getId()));

        assertThat(supportWorkflow.getDefinition()).contains(String.valueOf(salesAgent.getUuid()));
        assertThat(agentFacade.getDraftWorkflowId(restoredSalesAgent.getId())).isNotBlank();
    }

    @Test
    void testRestoringAnAgentWorkflowBringsBackOnlyThatAgent() {
        AiAgent supportAgent = createSupportAgent();
        long projectId = agentService.getProjectId(supportAgent);

        AiAgent salesAgent = agentFacade.createAgent("Sales Bot", "Sells", workspaceId, projectId)
            .agent();

        agentFacade.addAgentElement(
            salesAgent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        int publishedVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        publishProject(projectId);

        AiAgentDTO supportAgentDTO = agentFacade.getAgent(supportAgent.getId());

        agentFacade.updateAgent(supportAgent.getId(), null, null, "Be thorough.");
        agentFacade.updateAgentChannel(
            findChannel(supportAgentDTO, SLACK_CHANNEL_TYPE).getId(), Map.of("channel", "#sales"), 55L);
        agentFacade.updateAgent(salesAgent.getId(), null, null, "Sell harder.");

        agentContentContributor.restoreProjectWorkflowContent(
            projectId, publishedVersion, String.valueOf(supportAgent.getProjectWorkflowUuid()));

        AiAgentDTO restoredSupportAgentDTO = agentFacade.getAgent(supportAgent.getId());

        assertThat(restoredSupportAgentDTO.agent()
            .getInstructions()).isEqualTo("Be concise.");
        assertThat(findChannel(restoredSupportAgentDTO, SLACK_CHANNEL_TYPE)).satisfies(channel -> {
            assertThat(channel.getConnectionId()).isEqualTo(77L);
            assertThat(channel.getParameters()
                .get("channel")).isEqualTo("#support");
        });
        assertThat(agentService.getAgent(salesAgent.getId())
            .getInstructions())
                .as("restoring one agent workflow leaves the other agents alone")
                .isEqualTo("Sell harder.");

        Workflow supportWorkflow = workflowService.getWorkflow(agentFacade.getDraftWorkflowId(supportAgent.getId()));

        assertThat(supportWorkflow.getDefinition()).contains("Be concise.");
    }

    @Test
    void testRestoreRemovesNewerAgentsThatUseEachOther() {
        AiAgent supportAgent = createSupportAgent();
        long projectId = agentService.getProjectId(supportAgent);

        int publishedVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        publishProject(projectId);

        AiAgent helperAgent = agentFacade.createAgent("Helper Bot", null, workspaceId, projectId)
            .agent();
        AiAgent managerAgent = agentFacade.createAgent("Manager Bot", null, workspaceId, projectId)
            .agent();

        agentFacade.addAgentElement(
            managerAgent.getId(), AiAgentElement.KIND_SUB_AGENT, helperAgent.getId(), Map.of(), null);

        agentContentContributor.restoreProjectContent(projectId, publishedVersion);

        assertThat(agentService.getProjectAgents(projectId)).extracting(AiAgent::getTitle)
            .containsExactly("Support Bot");
    }

    @Test
    void testRestoringAnAgentKeepsItsLinkToARecreatedSubAgent() {
        AiAgent supportAgent = createSupportAgent();
        long projectId = agentService.getProjectId(supportAgent);

        AiAgent salesAgent = agentFacade.createAgent("Sales Bot", "Sells", workspaceId, projectId)
            .agent();

        agentFacade.addAgentElement(
            salesAgent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        publishProject(projectId);

        agentFacade.addAgentElement(
            supportAgent.getId(), AiAgentElement.KIND_SUB_AGENT, salesAgent.getId(), Map.of(), null);

        int publishedVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        publishProject(projectId);

        agentFacade.deleteAgentElement(
            findElement(agentFacade.getAgent(supportAgent.getId()), AiAgentElement.KIND_SUB_AGENT).getId());
        agentFacade.deleteAgent(salesAgent.getId());

        agentContentContributor.restoreProjectContent(projectId, publishedVersion);

        AiAgent recreatedSalesAgent = findByTitle(agentService.getProjectAgents(projectId), "Sales Bot");

        assertThat(recreatedSalesAgent.getId()).isNotEqualTo(salesAgent.getId());

        agentFacade.deleteAgentElement(
            findElement(agentFacade.getAgent(supportAgent.getId()), AiAgentElement.KIND_SUB_AGENT).getId());

        agentContentContributor.restoreProjectWorkflowContent(
            projectId, publishedVersion, String.valueOf(supportAgent.getProjectWorkflowUuid()));

        assertThat(findElement(agentFacade.getAgent(supportAgent.getId()), AiAgentElement.KIND_SUB_AGENT)
            .getReferenceId()).isEqualTo(recreatedSalesAgent.getId());
    }

    @Test
    void testPullUpdatesTheAgentNamedByAFileAndImportsANewOne() {
        AiAgent supportAgent = createSupportAgent();
        long projectId = agentService.getProjectId(supportAgent);

        Map<String, Object> exportedAgent = new HashMap<>(
            JsonUtils.readMap(agentFacade.exportAgent(supportAgent.getId())));

        exportedAgent.put("instructions", "Be thorough.");
        exportedAgent.put("channels", List.of());
        exportedAgent.put(
            "elements",
            List.of(Map.of("kind", AiAgentElement.KIND_MODEL, "parameters", Map.of("provider", "openai", "model",
                "gpt-5"))));

        Map<String, byte[]> files = Map.of(
            "agents/" + supportAgent.getName() + ".json", bytes(JsonUtils.write(exportedAgent)),
            "agents/new-bot.json", bytes(JsonUtils.write(Map.of("name", "new-bot", "title", "New Bot"))),
            "agents/nested/ignored.json", bytes(JsonUtils.write(Map.of("title", "Ignored"))));

        agentContentContributor.pullProjectContent(projectId, files);

        List<AiAgent> projectAgents = agentService.getProjectAgents(projectId);

        assertThat(projectAgents).extracting(AiAgent::getTitle)
            .containsExactlyInAnyOrder("Support Bot", "New Bot");
        assertThat(findByTitle(projectAgents, "New Bot").getName()).isEqualTo("new-bot");

        AiAgentDTO updatedAgentDTO = agentFacade.getAgent(supportAgent.getId());

        assertThat(updatedAgentDTO.agent()
            .getInstructions()).isEqualTo("Be thorough.");
        assertThat(updatedAgentDTO.channels()).extracting(AiAgentChannel::getChannelType)
            .as("the slack channel missing from the file is removed; the permanent ones stay")
            .doesNotContain(SLACK_CHANNEL_TYPE)
            .hasSize(2);

        Map<String, AiAgentElement> elementsByKind = updatedAgentDTO.elements()
            .stream()
            .collect(Collectors.toMap(AiAgentElement::getKind, element -> element));

        assertThat(elementsByKind).containsOnlyKeys(AiAgentElement.KIND_MODEL, AiAgentElement.KIND_SKILL);
        assertThat(elementsByKind.get(AiAgentElement.KIND_MODEL)
            .getParameters()
            .get("model")).isEqualTo("gpt-5");
        assertThat(elementsByKind.get(AiAgentElement.KIND_MODEL)
            .getConnectionId())
                .as("a matched element keeps its own connection")
                .isEqualTo(42L);
        assertThat(elementsByKind.get(AiAgentElement.KIND_SKILL)
            .getReferenceId()).isEqualTo(12345L);
    }

    private AiAgent createSupportAgent() {
        AiAgent agent = agentFacade.createAgent("Support Bot", "Answers questions", workspaceId, null)
            .agent();

        agentFacade.updateAgent(agent.getId(), null, null, "Be concise.");
        agentFacade.updateAgentSettings(agent.getId(), Map.of("temperature", 0.2));
        agentFacade.addAgentChannel(agent.getId(), SLACK_CHANNEL_TYPE, Map.of("channel", "#support"), 77L);
        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);
        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_SKILL, 12345L, Map.of(), null);

        return agentService.getAgent(agent.getId());
    }

    private int publishProject(long projectId) {
        int oldProjectVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        List<ProjectWorkflow> oldProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            projectId, oldProjectVersion);

        int newProjectVersion = projectService.publishProject(projectId, "first", false);

        for (ProjectWorkflow oldProjectWorkflow : oldProjectWorkflows) {
            String oldWorkflowId = oldProjectWorkflow.getWorkflowId();

            Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

            oldProjectWorkflow.setProjectVersion(newProjectVersion);
            oldProjectWorkflow.setWorkflowId(duplicatedWorkflow.getId());

            projectWorkflowService.publishWorkflow(projectId, oldProjectVersion, oldWorkflowId, oldProjectWorkflow);
        }

        return newProjectVersion;
    }

    private long createProject(String name) {
        Project project = new Project();

        project.setName(name);
        project.setWorkspaceId(workspaceId);

        return Objects.requireNonNull(projectService.create(project)
            .getId());
    }

    private List<String> agentWorkflowIds(long projectId) {
        Set<String> agentWorkflowUuids = agentService.getProjectAgents(projectId)
            .stream()
            .map(agent -> String.valueOf(agent.getProjectWorkflowUuid()))
            .collect(Collectors.toSet());

        return projectWorkflowService.getProjectWorkflows(projectId)
            .stream()
            .filter(projectWorkflow -> projectWorkflow.getType() == ProjectWorkflowType.AI_AGENT)
            .filter(projectWorkflow -> agentWorkflowUuids.contains(String.valueOf(projectWorkflow.getUuid())))
            .map(ProjectWorkflow::getWorkflowId)
            .toList();
    }

    private static AiAgentChannel findChannel(AiAgentDTO agentDTO, String channelType) {
        return agentDTO.channels()
            .stream()
            .filter(channel -> channelType.equals(channel.getChannelType()))
            .findFirst()
            .orElseThrow();
    }

    private static AiAgentElement findElement(AiAgentDTO agentDTO, String kind) {
        return agentDTO.elements()
            .stream()
            .filter(element -> kind.equals(element.getKind()))
            .findFirst()
            .orElseThrow();
    }

    private static AiAgent findByTitle(List<AiAgent> agents, String title) {
        return agents.stream()
            .filter(agent -> title.equals(agent.getTitle()))
            .findFirst()
            .orElseThrow();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
