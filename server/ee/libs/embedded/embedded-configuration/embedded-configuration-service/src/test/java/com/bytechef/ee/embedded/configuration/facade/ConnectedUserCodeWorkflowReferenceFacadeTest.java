/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflowConnection;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ConnectedUserCodeWorkflowReferenceFacadeTest {

    @Mock
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Mock
    private ConnectedUserProjectWorkflowConnectionRepository connectedUserProjectWorkflowConnectionRepository;

    @Mock
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Mock
    private ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;

    @Mock
    private ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver;

    @Mock
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    private ConnectedUserCodeWorkflowReferenceFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new ConnectedUserCodeWorkflowReferenceFacadeImpl(
            automationWorkflowProjectFacade, connectedUserProjectWorkflowConnectionRepository,
            connectedUserProjectWorkflowRepository, connectedUserProjectWorkflowManager,
            connectedUserWorkflowConnectionResolver, projectDeploymentFacade, projectDeploymentService,
            projectDeploymentWorkflowService, projectWorkflowService, workflowService);

        // Every case in this class provisions a template the connected user IS permitted to see; the rejections are
        // covered by ConnectedUserCodeWorkflowReferenceFacadeAuthorizationTest. Lenient because the cases that do not
        // provision (existing reference, enable/disable, dangling) never reach the catalog lookup.
        Mockito.lenient()
            .when(automationWorkflowProjectFacade.getPublishedProjects(Mockito.anyString(), Mockito.any()))
            .thenReturn(List.of(publishedCatalogProject("catalog-uuid")));
    }

    /**
     * This is the test that keeps two connected users' connections from leaking into each other, which is the entire
     * reason {@code WorkflowTestConfiguration} couldn't be reused for reference mode: each user's
     * {@link ConnectedUserCodeWorkflowReferenceFacadeImpl#getOrCreateReference} call must persist its OWN
     * {@link ConnectedUserProjectWorkflowConnection} rows, never sharing or overwriting the other user's wiring.
     */
    @Test
    void testTwoUsersReferencingTheSameCatalogWorkflowGetIndependentConnectionRows() {
        ProjectWorkflow catalogProjectWorkflow = new ProjectWorkflow(500L, 1, "catalog-wf-1");

        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid"))
            .thenReturn("catalog-wf-1");
        Mockito.when(projectWorkflowService.getWorkflowProjectWorkflow("catalog-wf-1"))
            .thenReturn(catalogProjectWorkflow);

        Workflow workflow = new Workflow(
            "{\"triggers\":[],\"tasks\":[{\"name\":\"t1\",\"type\":\"slack/v1/postMessage\"}]}", Workflow.Format.JSON);

        Mockito.when(workflowService.getWorkflow("catalog-wf-1"))
            .thenReturn(workflow);

        ConnectedUserProject userAProject = new ConnectedUserProject();

        userAProject.setId(10L);

        ConnectedUserProject userBProject = new ConnectedUserProject();

        userBProject.setId(20L);

        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userA"), Mockito.any()))
            .thenReturn(userAProject);
        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userB"), Mockito.any()))
            .thenReturn(userBProject);

        Mockito.when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(Mockito.anyLong(), Mockito.eq("catalog-uuid")))
            .thenReturn(Optional.empty());
        Mockito.when(connectedUserProjectWorkflowRepository.save(Mockito.any()))
            .thenAnswer(withGeneratedId());

        Mockito.when(connectedUserWorkflowConnectionResolver.resolve(workflow.getDefinition()))
            .thenReturn(Map.of("t1", 1L))
            .thenReturn(Map.of("t1", 2L));

        Mockito.when(projectDeploymentService.fetchProjectDeploymentByName(Mockito.eq(500L), Mockito.anyString()))
            .thenReturn(Optional.empty());
        Mockito.when(projectDeploymentFacade.createProjectDeployment(
            Mockito.any(), Mockito.eq("catalog-wf-1"), Mockito.anyList()))
            .thenReturn(900L, 901L);

        facade.getOrCreateReference("userA", "catalog-uuid", Environment.PRODUCTION);
        facade.getOrCreateReference("userB", "catalog-uuid", Environment.PRODUCTION);

        ArgumentCaptor<ConnectedUserProjectWorkflowConnection> captor =
            ArgumentCaptor.forClass(ConnectedUserProjectWorkflowConnection.class);

        Mockito.verify(connectedUserProjectWorkflowConnectionRepository, Mockito.times(2))
            .save(captor.capture());

        List<Long> wiredConnectionIds = captor.getAllValues()
            .stream()
            .map(ConnectedUserProjectWorkflowConnection::getConnectionId)
            .toList();

        Assertions.assertEquals(List.of(1L, 2L), wiredConnectionIds);
    }

    /**
     * Finding 2 regression test: the same connected user referencing the same catalog workflow in two different
     * {@link Environment}s must provision two DISTINCT
     * {@link com.bytechef.automation.configuration.domain.ProjectDeployment} rows -- one per environment -- rather than
     * colliding onto a single deployment stamped with whichever environment provisioned first.
     */
    @Test
    void testGetOrCreateReferenceProvisionsDistinctDeploymentsPerEnvironment() {
        ProjectWorkflow catalogProjectWorkflow = new ProjectWorkflow(500L, 1, "catalog-wf-1");

        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid"))
            .thenReturn("catalog-wf-1");
        Mockito.when(projectWorkflowService.getWorkflowProjectWorkflow("catalog-wf-1"))
            .thenReturn(catalogProjectWorkflow);

        Workflow workflow = new Workflow(
            "{\"triggers\":[],\"tasks\":[]}", Workflow.Format.JSON);

        Mockito.when(workflowService.getWorkflow("catalog-wf-1"))
            .thenReturn(workflow);
        Mockito.when(connectedUserWorkflowConnectionResolver.resolve(workflow.getDefinition()))
            .thenReturn(Map.of());

        ConnectedUserProject productionProject = new ConnectedUserProject();

        productionProject.setId(10L);

        ConnectedUserProject developmentProject = new ConnectedUserProject();

        developmentProject.setId(11L);

        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            "userA", Environment.PRODUCTION))
            .thenReturn(productionProject);
        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            "userA", Environment.DEVELOPMENT))
            .thenReturn(developmentProject);

        Mockito.when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(Mockito.anyLong(), Mockito.eq("catalog-uuid")))
            .thenReturn(Optional.empty());
        Mockito.when(connectedUserProjectWorkflowRepository.save(Mockito.any()))
            .thenAnswer(withGeneratedId());

        // Names differ by environment, so each lookup misses and a fresh deployment is created.
        Mockito.when(projectDeploymentService.fetchProjectDeploymentByName(Mockito.eq(500L), Mockito.anyString()))
            .thenReturn(Optional.empty());

        ArgumentCaptor<ProjectDeployment> projectDeploymentCaptor = ArgumentCaptor.forClass(ProjectDeployment.class);

        Mockito.when(projectDeploymentFacade.createProjectDeployment(
            projectDeploymentCaptor.capture(), Mockito.eq("catalog-wf-1"), Mockito.anyList()))
            .thenReturn(900L, 901L);

        facade.getOrCreateReference("userA", "catalog-uuid", Environment.PRODUCTION);
        facade.getOrCreateReference("userA", "catalog-uuid", Environment.DEVELOPMENT);

        List<ProjectDeployment> createdDeployments = projectDeploymentCaptor.getAllValues();

        Assertions.assertEquals(2, createdDeployments.size());

        ProjectDeployment productionDeployment = createdDeployments.get(0);
        ProjectDeployment developmentDeployment = createdDeployments.get(1);

        Assertions.assertEquals(Environment.PRODUCTION, productionDeployment.getEnvironment());
        Assertions.assertEquals(Environment.DEVELOPMENT, developmentDeployment.getEnvironment());

        // Distinct names is what makes the (catalogProjectId, name) lookup unable to collide across environments.
        Assertions.assertNotEquals(productionDeployment.getName(), developmentDeployment.getName());
    }

    @Test
    void testGetOrCreateReferenceReturnsExistingRowWithoutReprovisioning() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);

        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userA"), Mockito.any()))
            .thenReturn(connectedUserProject);

        ConnectedUserProjectWorkflow existingReference = new ConnectedUserProjectWorkflow();

        existingReference.setId(1L);

        Mockito.when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(10L, "catalog-uuid"))
            .thenReturn(Optional.of(existingReference));

        ConnectedUserProjectWorkflow result = facade.getOrCreateReference(
            "userA", "catalog-uuid", Environment.PRODUCTION);

        Assertions.assertSame(existingReference, result);

        // automationWorkflowProjectFacade included deliberately: the provisioning-time permission check must not run
        // for an already-provisioned reference, so a narrowed permission expression cannot break a running automation.
        Mockito.verifyNoInteractions(
            automationWorkflowProjectFacade, projectWorkflowService, workflowService,
            connectedUserWorkflowConnectionResolver, projectDeploymentFacade, projectDeploymentService);
    }

    /**
     * A component with no matching connection for the connected user must not abort provisioning outright: the
     * reference row is still created -- left {@code enabled = false} -- and {@link MissingConnectionException} is
     * rethrown afterward so the caller can surface which connection is missing and let the connected user fix it.
     */
    @Test
    void testMissingConnectionStillCreatesDisabledReferenceAndRethrows() {
        ProjectWorkflow catalogProjectWorkflow = new ProjectWorkflow(500L, 1, "catalog-wf-1");

        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid"))
            .thenReturn("catalog-wf-1");
        Mockito.when(projectWorkflowService.getWorkflowProjectWorkflow("catalog-wf-1"))
            .thenReturn(catalogProjectWorkflow);

        Workflow workflow = new Workflow(
            "{\"triggers\":[],\"tasks\":[{\"name\":\"t1\",\"type\":\"slack/v1/postMessage\"}]}", Workflow.Format.JSON);

        Mockito.when(workflowService.getWorkflow("catalog-wf-1"))
            .thenReturn(workflow);

        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);

        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userA"), Mockito.any()))
            .thenReturn(connectedUserProject);

        Mockito.when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(10L, "catalog-uuid"))
            .thenReturn(Optional.empty());
        Mockito.when(connectedUserProjectWorkflowRepository.save(Mockito.any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Mockito.when(connectedUserWorkflowConnectionResolver.resolve(workflow.getDefinition()))
            .thenThrow(new MissingConnectionException("slack"));

        Mockito.when(projectDeploymentService.fetchProjectDeploymentByName(Mockito.eq(500L), Mockito.anyString()))
            .thenReturn(Optional.empty());
        Mockito.when(projectDeploymentFacade.createProjectDeployment(
            Mockito.any(), Mockito.eq("catalog-wf-1"), Mockito.eq(List.of())))
            .thenReturn(900L);

        MissingConnectionException thrown = Assertions.assertThrows(
            MissingConnectionException.class,
            () -> facade.getOrCreateReference("userA", "catalog-uuid", Environment.PRODUCTION));

        Assertions.assertEquals("slack", thrown.getComponentName());

        ArgumentCaptor<ConnectedUserProjectWorkflow> captor =
            ArgumentCaptor.forClass(ConnectedUserProjectWorkflow.class);

        Mockito.verify(connectedUserProjectWorkflowRepository)
            .save(captor.capture());

        ConnectedUserProjectWorkflow saved = captor.getValue();

        Assertions.assertFalse(saved.isEnabled());
        Assertions.assertEquals("catalog-uuid", saved.getCatalogWorkflowUuid());
        Assertions.assertNull(saved.getProjectWorkflowId());

        Mockito.verify(connectedUserProjectWorkflowConnectionRepository, Mockito.never())
            .save(Mockito.any());
    }

    @Test
    void testEnableReferenceTogglesEnabledFlagAndProjectDeploymentWorkflow() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);

        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userA"), Mockito.any()))
            .thenReturn(connectedUserProject);

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(1L);
        reference.setConnectedUserProjectId(10L);
        reference.setCatalogWorkflowUuid("catalog-uuid");
        reference.setProjectDeploymentId(900L);
        reference.setEnabled(false);

        Mockito.when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(10L, "catalog-uuid"))
            .thenReturn(Optional.of(reference));

        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid"))
            .thenReturn("catalog-wf-1");

        Workflow workflow = new Workflow(
            "{\"triggers\":[],\"tasks\":[{\"name\":\"t1\",\"type\":\"slack/v1/postMessage\"}]}", Workflow.Format.JSON);

        Mockito.when(workflowService.getWorkflow("catalog-wf-1"))
            .thenReturn(workflow);
        Mockito.when(connectedUserWorkflowConnectionResolver.resolve(workflow.getDefinition()))
            .thenReturn(Map.of("t1", 1L));
        Mockito.when(connectedUserProjectWorkflowConnectionRepository.findAllByConnectedUserProjectWorkflowId(1L))
            .thenReturn(List.of());
        Mockito.when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(900L, "catalog-wf-1"))
            .thenReturn(new ProjectDeploymentWorkflow());

        facade.enableReference("userA", "catalog-uuid", true, Environment.PRODUCTION);

        Assertions.assertTrue(reference.isEnabled());
        Mockito.verify(connectedUserProjectWorkflowRepository)
            .save(reference);
        Mockito.verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(900L, "catalog-wf-1", true);
    }

    /**
     * Finding 3 regression test (fixed-then-enable): provisioning previously failed with
     * {@link MissingConnectionException} and left the reference disabled with no wiring. The connected user has since
     * created the missing "slack" connection, so re-enabling must re-run resolution, populate BOTH the
     * {@link ConnectedUserProjectWorkflowConnection} bookkeeping rows and the real {@link ProjectDeploymentWorkflow}
     * execution-time connections, and then proceed to enable -- never leaving the workflow running with stale/absent
     * wiring.
     */
    @Test
    void testEnableReferenceRewiresConnectionsWhenPreviouslyMissingConnectionWasFixed() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);

        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userA"), Mockito.any()))
            .thenReturn(connectedUserProject);

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(1L);
        reference.setConnectedUserProjectId(10L);
        reference.setCatalogWorkflowUuid("catalog-uuid");
        reference.setProjectDeploymentId(900L);
        reference.setEnabled(false);

        Mockito.when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(10L, "catalog-uuid"))
            .thenReturn(Optional.of(reference));

        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid"))
            .thenReturn("catalog-wf-1");

        Workflow workflow = new Workflow(
            "{\"triggers\":[],\"tasks\":[{\"name\":\"t1\",\"type\":\"slack/v1/postMessage\"}]}", Workflow.Format.JSON);

        Mockito.when(workflowService.getWorkflow("catalog-wf-1"))
            .thenReturn(workflow);

        // The connection is now resolvable -- the connected user created it after the earlier
        // MissingConnectionException.
        Mockito.when(connectedUserWorkflowConnectionResolver.resolve(workflow.getDefinition()))
            .thenReturn(Map.of("t1", 42L));

        // No bookkeeping rows exist yet, since the original provisioning never got past MissingConnectionException.
        Mockito.when(connectedUserProjectWorkflowConnectionRepository.findAllByConnectedUserProjectWorkflowId(1L))
            .thenReturn(List.of());

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        Mockito.when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(900L, "catalog-wf-1"))
            .thenReturn(projectDeploymentWorkflow);

        facade.enableReference("userA", "catalog-uuid", true, Environment.PRODUCTION);

        ArgumentCaptor<ConnectedUserProjectWorkflowConnection> bookkeepingCaptor =
            ArgumentCaptor.forClass(ConnectedUserProjectWorkflowConnection.class);

        Mockito.verify(connectedUserProjectWorkflowConnectionRepository)
            .save(bookkeepingCaptor.capture());

        ConnectedUserProjectWorkflowConnection savedBookkeepingConnection = bookkeepingCaptor.getValue();

        Assertions.assertEquals("t1", savedBookkeepingConnection.getWorkflowNodeName());
        Assertions.assertEquals(42L, savedBookkeepingConnection.getConnectionId());

        ArgumentCaptor<ProjectDeploymentWorkflow> projectDeploymentWorkflowCaptor =
            ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

        Mockito.verify(projectDeploymentWorkflowService)
            .update(projectDeploymentWorkflowCaptor.capture());

        List<ProjectDeploymentWorkflowConnection> updatedConnections = projectDeploymentWorkflowCaptor.getValue()
            .getConnections();

        Assertions.assertEquals(1, updatedConnections.size());
        Assertions.assertEquals(42L, updatedConnections.get(0)
            .getConnectionId());

        Assertions.assertTrue(reference.isEnabled());
        Mockito.verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(900L, "catalog-wf-1", true);
    }

    /**
     * Finding 3 regression test (still-missing-then-enable): the connected user has NOT created the missing connection
     * yet, so re-running resolution on enable must still throw {@link MissingConnectionException}, and enabling must
     * never succeed -- the reference is left disabled and neither {@link ConnectedUserProjectWorkflowRepository#save}
     * nor {@link com.bytechef.automation.configuration.facade.ProjectDeploymentFacade#enableProjectDeploymentWorkflow}
     * is ever called.
     */
    @Test
    void testEnableReferenceStillThrowsWhenConnectionIsStillMissing() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);

        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userA"), Mockito.any()))
            .thenReturn(connectedUserProject);

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(1L);
        reference.setConnectedUserProjectId(10L);
        reference.setCatalogWorkflowUuid("catalog-uuid");
        reference.setProjectDeploymentId(900L);
        reference.setEnabled(false);

        Mockito.when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(10L, "catalog-uuid"))
            .thenReturn(Optional.of(reference));

        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid"))
            .thenReturn("catalog-wf-1");

        Workflow workflow = new Workflow(
            "{\"triggers\":[],\"tasks\":[{\"name\":\"t1\",\"type\":\"slack/v1/postMessage\"}]}", Workflow.Format.JSON);

        Mockito.when(workflowService.getWorkflow("catalog-wf-1"))
            .thenReturn(workflow);
        Mockito.when(connectedUserWorkflowConnectionResolver.resolve(workflow.getDefinition()))
            .thenThrow(new MissingConnectionException("slack"));

        MissingConnectionException thrown = Assertions.assertThrows(
            MissingConnectionException.class,
            () -> facade.enableReference("userA", "catalog-uuid", true, Environment.PRODUCTION));

        Assertions.assertEquals("slack", thrown.getComponentName());
        Assertions.assertFalse(reference.isEnabled());

        Mockito.verify(connectedUserProjectWorkflowConnectionRepository, Mockito.never())
            .save(Mockito.any());
        Mockito.verify(connectedUserProjectWorkflowRepository, Mockito.never())
            .save(Mockito.any());
        Mockito.verify(projectDeploymentWorkflowService, Mockito.never())
            .update(Mockito.any(ProjectDeploymentWorkflow.class));
        Mockito.verify(projectDeploymentFacade, Mockito.never())
            .enableProjectDeploymentWorkflow(Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    void testEnableReferenceThrowsWhenNoReferenceExists() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);

        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userA"), Mockito.any()))
            .thenReturn(connectedUserProject);

        Mockito.when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(10L, "catalog-uuid"))
            .thenReturn(Optional.empty());

        Assertions.assertThrows(
            ConfigurationException.class,
            () -> facade.enableReference("userA", "catalog-uuid", true, Environment.PRODUCTION));
    }

    @Test
    void testDeleteReferenceRemovesConnectionsAndReferenceRow() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);

        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userA"), Mockito.any()))
            .thenReturn(connectedUserProject);

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(1L);
        reference.setConnectedUserProjectId(10L);
        reference.setCatalogWorkflowUuid("catalog-uuid");

        Mockito.when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(10L, "catalog-uuid"))
            .thenReturn(Optional.of(reference));

        ConnectedUserProjectWorkflowConnection connection =
            new ConnectedUserProjectWorkflowConnection(5L, 1L, "t1", 1L, 0);

        Mockito.when(connectedUserProjectWorkflowConnectionRepository.findAllByConnectedUserProjectWorkflowId(1L))
            .thenReturn(List.of(connection));

        facade.deleteReference("userA", "catalog-uuid", Environment.PRODUCTION);

        Mockito.verify(connectedUserProjectWorkflowConnectionRepository)
            .deleteById(5L);
        Mockito.verify(connectedUserProjectWorkflowRepository)
            .deleteById(1L);
    }

    /**
     * The uuid-stability fix means a redeploy that keeps every workflow name unchanged must not dangle any reference:
     * only a reference whose catalog workflow was genuinely removed from the artifact gets flagged.
     */
    @Test
    void testMarkDanglingReferencesFlagsOnlyReferencesRemovedFromTheCatalog() {
        ConnectedUserProjectWorkflow stillPresent = referenceRow(1L, "uuid-present");
        ConnectedUserProjectWorkflow removed = referenceRow(2L, "uuid-removed");
        ConnectedUserProjectWorkflow copyModeRow = new ConnectedUserProjectWorkflow();

        copyModeRow.setId(3L);
        copyModeRow.setConnectedUserProjectId(30L);
        copyModeRow.setProjectWorkflowId(999L);

        Mockito.when(connectedUserProjectWorkflowRepository.findAll())
            .thenReturn(List.of(stillPresent, removed, copyModeRow));

        // Redeploy that carries every workflow name (and therefore uuid) forward unchanged.
        facade.markDanglingReferences(500L, Set.of("uuid-present", "uuid-removed"), Set.of("uuid-present"));

        Assertions.assertFalse(stillPresent.isDangling());
        Mockito.verify(connectedUserProjectWorkflowRepository, Mockito.never())
            .save(stillPresent);

        Assertions.assertTrue(removed.isDangling());
        Assertions.assertEquals("Removed from the catalog project on redeploy", removed.getDanglingReason());
        Mockito.verify(connectedUserProjectWorkflowRepository)
            .save(removed);

        // A copy-mode row (no catalogWorkflowUuid) must never be touched by dangling detection.
        Mockito.verify(connectedUserProjectWorkflowRepository, Mockito.never())
            .save(copyModeRow);
    }

    /**
     * Finding 1 regression test: {@code markDanglingReferences} must dangle exactly {@code previous \ current} for THIS
     * catalog project, never guessing off the repository-wide set of all references. A reference belonging to a
     * different catalog project (e.g. "acme-crm") must never be touched by a redeploy of another catalog project (e.g.
     * "acme-billing"), even though both rows are returned by the same {@code findAll()} call.
     */
    @Test
    void testMarkDanglingReferencesNeverTouchesAnotherCatalogProjectsReferences() {
        ConnectedUserProjectWorkflow billingRemoved = referenceRow(1L, "billing-uuid-removed");
        ConnectedUserProjectWorkflow billingStillPresent = referenceRow(2L, "billing-uuid-present");
        ConnectedUserProjectWorkflow crmReference = referenceRow(3L, "crm-uuid-untouched");

        Mockito.when(connectedUserProjectWorkflowRepository.findAll())
            .thenReturn(List.of(billingRemoved, billingStillPresent, crmReference));

        // Redeploy of "acme-billing" only: its previous/current sets say nothing about "acme-crm"'s uuid, so
        // crmReference must never be considered even though currentCatalogWorkflowUuids doesn't contain it either.
        facade.markDanglingReferences(
            500L, Set.of("billing-uuid-removed", "billing-uuid-present"), Set.of("billing-uuid-present"));

        Assertions.assertTrue(billingRemoved.isDangling());
        Mockito.verify(connectedUserProjectWorkflowRepository)
            .save(billingRemoved);

        Assertions.assertFalse(billingStillPresent.isDangling());
        Mockito.verify(connectedUserProjectWorkflowRepository, Mockito.never())
            .save(billingStillPresent);

        Assertions.assertFalse(crmReference.isDangling());
        Mockito.verify(connectedUserProjectWorkflowRepository, Mockito.never())
            .save(crmReference);
    }

    private static AutomationWorkflowProjectDTO publishedCatalogProject(String workflowUuid) {
        return new AutomationWorkflowProjectDTO(
            500L, "Catalog", "", null, List.of(), true, 1, 1,
            List.of(
                new ConnectedUserWorkflowTemplateDTO(
                    workflowUuid, "Label", "Description", null, List.of(), List.of(), null)),
            null, true);
    }

    /**
     * Simulates the id a real {@code save(...)} call would generate, since the production code reads
     * {@code saved.getId()} right after saving to stamp it onto the per-node connection rows.
     */
    private static Answer<ConnectedUserProjectWorkflow> withGeneratedId() {
        AtomicLong nextId = new AtomicLong(1L);

        return invocation -> {
            ConnectedUserProjectWorkflow connectedUserProjectWorkflow = invocation.getArgument(0);

            connectedUserProjectWorkflow.setId(nextId.getAndIncrement());

            return connectedUserProjectWorkflow;
        };
    }

    private static ConnectedUserProjectWorkflow referenceRow(long id, String catalogWorkflowUuid) {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setId(id);
        connectedUserProjectWorkflow.setConnectedUserProjectId(10L);
        connectedUserProjectWorkflow.setCatalogWorkflowUuid(catalogWorkflowUuid);

        return connectedUserProjectWorkflow;
    }
}
