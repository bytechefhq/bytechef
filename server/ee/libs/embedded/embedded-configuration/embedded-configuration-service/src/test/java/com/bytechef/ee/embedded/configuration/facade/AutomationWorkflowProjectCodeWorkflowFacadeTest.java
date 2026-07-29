/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

/**
 * Verifies {@link AutomationWorkflowProjectCodeWorkflowFacadeImpl#save}: the first deploy of a code workflow project
 * must resolve/create its catalog {@link Project} through {@link AutomationWorkflowProjectFacade}'s marker convention,
 * rather than through {@link AutomationWorkflowProjectFacade#createProject} being called a second time or a bare
 * {@link ProjectService} lookup.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AutomationWorkflowProjectCodeWorkflowFacadeTest {

    @Mock
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Mock
    private CodeWorkflowContainerFacade codeWorkflowContainerFacade;

    @Mock
    private CodeWorkflowContainerService codeWorkflowContainerService;

    @Mock
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Mock
    private ProjectCodeWorkflowService projectCodeWorkflowService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    private AutomationWorkflowProjectCodeWorkflowFacadeImpl facade;

    @BeforeEach
    void setUp() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        facade = new AutomationWorkflowProjectCodeWorkflowFacadeImpl(
            applicationProperties, mock(CacheManager.class), automationWorkflowProjectFacade,
            codeWorkflowContainerFacade, codeWorkflowContainerService, connectedUserCodeWorkflowReferenceFacade,
            projectCodeWorkflowService, projectService, projectWorkflowService);
    }

    @Test
    void testFirstDeployCreatesTheCatalogProject() {
        Mockito.when(automationWorkflowProjectFacade.fetchProjectIdByName("acme-billing"))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.createProject(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.isNull(), Mockito.eq(List.of()), Mockito.isNull()))
            .thenReturn(100L);

        Project project = new Project();

        project.setId(100L);

        Mockito.when(projectService.getProject(100L))
            .thenReturn(project);

        CodeWorkflowContainer container = codeWorkflowContainer(Map.of("charge", "wf-1"));

        Mockito.when(codeWorkflowContainerFacade.create(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.any(), Mockito.eq(Language.JAVASCRIPT),
            Mockito.any(), Mockito.eq(PlatformType.AUTOMATION)))
            .thenReturn(container);

        facade.save(fakeProjectDefinitionBytes("acme-billing"), Language.JAVASCRIPT);

        Mockito.verify(projectWorkflowService)
            .addWorkflow(100L, project.getLastProjectVersion(), "wf-1");
        Mockito.verify(projectService)
            .publishProject(100L, null, false);

        // Publish must go straight through ProjectService, exactly like the plain automation code-workflow deploy
        // path -- NOT through AutomationWorkflowProjectFacade#publishProject, which additionally duplicates workflow
        // rows for the visual-editor versioning story that code workflows don't need.
        Mockito.verify(automationWorkflowProjectFacade, Mockito.never())
            .publishProject(Mockito.anyLong());
    }

    /**
     * Verifies the dangling-detection wiring: after {@code save} publishes the project, it must call
     * {@link ConnectedUserCodeWorkflowReferenceFacade#markDanglingReferences} with the newly-published project id and
     * the uuid set of the workflows that are actually live in that published version -- otherwise a reference to a
     * workflow removed from the catalog would never be flagged.
     */
    @Test
    void testSaveMarksDanglingReferencesWithPublishedProjectWorkflowUuids() {
        Mockito.when(automationWorkflowProjectFacade.fetchProjectIdByName("acme-billing"))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.createProject(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.isNull(), Mockito.eq(List.of()), Mockito.isNull()))
            .thenReturn(100L);

        Project project = new Project();

        project.setId(100L);

        Mockito.when(projectService.getProject(100L))
            .thenReturn(project);

        CodeWorkflowContainer container = codeWorkflowContainer(Map.of("charge", "wf-1"));

        Mockito.when(codeWorkflowContainerFacade.create(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.any(), Mockito.eq(Language.JAVASCRIPT),
            Mockito.any(), Mockito.eq(PlatformType.AUTOMATION)))
            .thenReturn(container);

        UUID chargeUuid = UUID.randomUUID();

        ProjectWorkflow publishedProjectWorkflow = new ProjectWorkflow(
            100L, project.getLastProjectVersion(), "wf-1", chargeUuid);

        Mockito.when(projectWorkflowService.getProjectWorkflows(100L, project.getLastProjectVersion()))
            .thenReturn(List.of(publishedProjectWorkflow));

        facade.save(fakeProjectDefinitionBytes("acme-billing"), Language.JAVASCRIPT);

        // First deploy of this catalog project: there is no previous published version, so the previous-uuid set
        // must be empty.
        Mockito.verify(connectedUserCodeWorkflowReferenceFacade)
            .markDanglingReferences(100L, Set.of(), Set.of(chargeUuid.toString()));
    }

    /**
     * Verifies the uuid carry-forward invariant: {@link ProjectWorkflowService#addWorkflow} always mints a fresh uuid,
     * so a workflow whose name is unchanged across a redeploy must have that fresh uuid overwritten with the uuid the
     * previous deploy's same-named {@link ProjectWorkflow} row carried -- otherwise per-user references pinned on
     * {@code catalog_workflow_uuid} would go dangling on every redeploy.
     */
    @Test
    void testRedeployWithUnchangedWorkflowNameCarriesUuidForward() {
        Mockito.when(automationWorkflowProjectFacade.fetchProjectIdByName("acme-billing"))
            .thenReturn(Optional.of(100L));

        Project project = new Project();

        project.setId(100L);

        Mockito.when(projectService.getProject(100L))
            .thenReturn(project);

        ProjectCodeWorkflow previousProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        Mockito.when(previousProjectCodeWorkflow.getCodeWorkflowContainerId())
            .thenReturn(55L);
        Mockito.when(projectCodeWorkflowService.getProjectCodeWorkflow(100L))
            .thenReturn(previousProjectCodeWorkflow);

        CodeWorkflowContainer previousContainer = codeWorkflowContainer(Map.of("charge", "wf-old-1"));

        Mockito.when(codeWorkflowContainerService.getCodeWorkflowContainer(55L))
            .thenReturn(previousContainer);

        UUID previousUuid = UUID.randomUUID();

        ProjectWorkflow previousProjectWorkflow = new ProjectWorkflow(100L, 1, "wf-old-1", previousUuid);

        Mockito.when(projectWorkflowService.getWorkflowProjectWorkflow("wf-old-1"))
            .thenReturn(previousProjectWorkflow);

        CodeWorkflowContainer newContainer = codeWorkflowContainer(Map.of("charge", "wf-new-1"));

        Mockito.when(codeWorkflowContainerFacade.create(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.any(), Mockito.eq(Language.JAVASCRIPT),
            Mockito.any(), Mockito.eq(PlatformType.AUTOMATION)))
            .thenReturn(newContainer);

        ProjectWorkflow savedProjectWorkflow = new ProjectWorkflow(200L);

        Mockito.when(projectWorkflowService.addWorkflow(100L, project.getLastProjectVersion(), "wf-new-1"))
            .thenReturn(savedProjectWorkflow);

        facade.save(fakeProjectDefinitionBytes("acme-billing"), Language.JAVASCRIPT);

        Mockito.verify(projectWorkflowService)
            .update(savedProjectWorkflow);
        Assertions.assertEquals(previousUuid, savedProjectWorkflow.getUuid());
    }

    /**
     * Companion to {@link #testRedeployWithUnchangedWorkflowNameCarriesUuidForward}: a redeploy that both keeps an
     * existing workflow name and introduces a new one must carry the uuid forward only for the unchanged name -- the
     * genuinely new name gets the fresh uuid {@link ProjectWorkflowService#addWorkflow} minted, untouched.
     */
    @Test
    void testRedeployWithNewWorkflowNameGetsFreshUuidWhileExistingNameCarriesForward() {
        Mockito.when(automationWorkflowProjectFacade.fetchProjectIdByName("acme-billing"))
            .thenReturn(Optional.of(100L));

        Project project = new Project();

        project.setId(100L);

        Mockito.when(projectService.getProject(100L))
            .thenReturn(project);

        ProjectCodeWorkflow previousProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        Mockito.when(previousProjectCodeWorkflow.getCodeWorkflowContainerId())
            .thenReturn(55L);
        Mockito.when(projectCodeWorkflowService.getProjectCodeWorkflow(100L))
            .thenReturn(previousProjectCodeWorkflow);

        CodeWorkflowContainer previousContainer = codeWorkflowContainer(Map.of("charge", "wf-old-1"));

        Mockito.when(codeWorkflowContainerService.getCodeWorkflowContainer(55L))
            .thenReturn(previousContainer);

        UUID previousUuid = UUID.randomUUID();

        ProjectWorkflow previousProjectWorkflow = new ProjectWorkflow(100L, 1, "wf-old-1", previousUuid);

        Mockito.when(projectWorkflowService.getWorkflowProjectWorkflow("wf-old-1"))
            .thenReturn(previousProjectWorkflow);

        CodeWorkflowContainer newContainer = codeWorkflowContainer(
            Map.of("charge", "wf-new-1", "refund", "wf-new-2"));

        Mockito.when(codeWorkflowContainerFacade.create(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.any(), Mockito.eq(Language.JAVASCRIPT),
            Mockito.any(), Mockito.eq(PlatformType.AUTOMATION)))
            .thenReturn(newContainer);

        ProjectWorkflow chargeProjectWorkflow = new ProjectWorkflow(201L);
        ProjectWorkflow refundProjectWorkflow = new ProjectWorkflow(202L);

        Mockito.when(projectWorkflowService.addWorkflow(100L, project.getLastProjectVersion(), "wf-new-1"))
            .thenReturn(chargeProjectWorkflow);
        Mockito.when(projectWorkflowService.addWorkflow(100L, project.getLastProjectVersion(), "wf-new-2"))
            .thenReturn(refundProjectWorkflow);

        facade.save(fakeProjectDefinitionBytesTwoWorkflows("acme-billing"), Language.JAVASCRIPT);

        Mockito.verify(projectWorkflowService)
            .update(chargeProjectWorkflow);
        Assertions.assertEquals(previousUuid, chargeProjectWorkflow.getUuid());

        Mockito.verify(projectWorkflowService, Mockito.never())
            .update(refundProjectWorkflow);
        Assertions.assertNull(refundProjectWorkflow.getUuid());
    }

    @Test
    void testSaveReturnsWarningForWorkflowWithNoPubliclyInvocableTrigger() {
        Mockito.when(automationWorkflowProjectFacade.fetchProjectIdByName("acme-billing"))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.createProject(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.isNull(), Mockito.eq(List.of()), Mockito.isNull()))
            .thenReturn(100L);

        Project project = new Project();

        project.setId(100L);

        Mockito.when(projectService.getProject(100L))
            .thenReturn(project);

        CodeWorkflowContainer container = codeWorkflowContainer(Map.of("charge", "wf-1"));

        Mockito.when(codeWorkflowContainerFacade.create(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.any(), Mockito.eq(Language.JAVASCRIPT),
            Mockito.any(), Mockito.eq(PlatformType.AUTOMATION)))
            .thenReturn(container);

        List<String> warnings = facade.save(fakeProjectDefinitionBytes("acme-billing"), Language.JAVASCRIPT);

        Assertions.assertEquals(1, warnings.size());
        Assertions.assertTrue(warnings.getFirst()
            .contains("will not be invocable"));
    }

    private static CodeWorkflowContainer codeWorkflowContainer(Map<String, String> workflowNameIds) {
        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        Mockito.when(codeWorkflowContainer.getWorkflowNameIds())
            .thenReturn(workflowNameIds);

        return codeWorkflowContainer;
    }

    // The newlines are embedded JavaScript source content, not console formatting; %n would corrupt the script.
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static byte[] fakeProjectDefinitionBytes(String name) {
        String source = """
            ({
                name: "%s",
                version: "1",
                description: "A code workflow.",
                workflows: [
                    {
                        name: "charge",
                        label: "Charge",
                        tasks: [
                            {
                                name: "my-task",
                                label: "My Task",
                                perform: function () {
                                    return "hello";
                                }
                            }
                        ]
                    }
                ]
            })
            """.formatted(name);

        return source.getBytes(StandardCharsets.UTF_8);
    }

    // The newlines are embedded JavaScript source content, not console formatting; %n would corrupt the script.
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static byte[] fakeProjectDefinitionBytesTwoWorkflows(String name) {
        String source = """
            ({
                name: "%s",
                version: "1",
                description: "A code workflow.",
                workflows: [
                    {
                        name: "charge",
                        label: "Charge",
                        tasks: [
                            {
                                name: "my-task",
                                label: "My Task",
                                perform: function () {
                                    return "hello";
                                }
                            }
                        ]
                    },
                    {
                        name: "refund",
                        label: "Refund",
                        tasks: [
                            {
                                name: "my-task",
                                label: "My Task",
                                perform: function () {
                                    return "hello";
                                }
                            }
                        ]
                    }
                ]
            })
            """.formatted(name);

        return source.getBytes(StandardCharsets.UTF_8);
    }
}
