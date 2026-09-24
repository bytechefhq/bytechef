/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ReferenceResolution;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.RowSpec;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserReferenceRolloutManagerTest {

    private static final long AUTOMATION_WORKFLOW_PROJECT_ID = 500L;
    private static final String FIRST_UUID = UUID.randomUUID()
        .toString();
    private static final String SECOND_UUID = UUID.randomUUID()
        .toString();

    private final ConnectedUserProjectService connectedUserProjectService = mock(ConnectedUserProjectService.class);
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository = mock(
        ConnectedUserProjectWorkflowRepository.class);
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager = mock(
        ConnectedUserReferenceDeploymentManager.class);
    private final PlatformTransactionManager platformTransactionManager = mock(PlatformTransactionManager.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

    private ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;

    @BeforeEach
    void setUp() {
        when(platformTransactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        connectedUserReferenceRolloutManager = new ConnectedUserReferenceRolloutManager(
            connectedUserProjectService, connectedUserProjectWorkflowRepository,
            connectedUserReferenceDeploymentManager, platformTransactionManager, projectDeploymentService,
            projectWorkflowService);

        ConnectedUserProject connectedUserProject = mock(ConnectedUserProject.class);

        when(connectedUserProject.getConnectedUserId()).thenReturn(7L);
        when(connectedUserProjectService.getConnectedUserProject(anyLong())).thenReturn(connectedUserProject);
        when(connectedUserReferenceDeploymentManager.getLastPublishedVersion(AUTOMATION_WORKFLOW_PROJECT_ID))
            .thenReturn(2);
        when(connectedUserReferenceDeploymentManager.fetchRow(anyLong(), anyString())).thenReturn(Optional.empty());
        when(connectedUserReferenceDeploymentManager.getWorkflowId(eq(AUTOMATION_WORKFLOW_PROJECT_ID), eq(2),
            anyString()))
                .thenAnswer(invocation -> "workflow-" + invocation.getArgument(2));
        when(connectedUserReferenceDeploymentManager.resolveReference(
            anyLong(), anyString(), anyBoolean(), anyMap(), anyList(), anyMap()))
                .thenReturn(new ReferenceResolution(
                    new RowSpec(new ResolvedWorkflowConnections(List.of(), List.of()), true, null), null, null));
        when(projectWorkflowService.getProjectWorkflows(AUTOMATION_WORKFLOW_PROJECT_ID, 2))
            .thenReturn(List.of(projectWorkflow(FIRST_UUID), projectWorkflow(SECOND_UUID)));
    }

    @Test
    void testRollOutLogsAndReturnsWhenTheDeploymentsCannotBeListed() {
        when(projectDeploymentService.getAllProjectDeployments(AUTOMATION_WORKFLOW_PROJECT_ID))
            .thenThrow(new IllegalStateException("Database unavailable"));

        ListAppender<ILoggingEvent> appender = attachAppenderToRolloutServiceLogger();

        try {
            assertThatCode(() -> connectedUserReferenceRolloutManager.rollOut(AUTOMATION_WORKFLOW_PROJECT_ID))
                .doesNotThrowAnyException();

            verifyNoInteractions(connectedUserProjectWorkflowRepository);
            assertThat(appender.list)
                .singleElement()
                .extracting(ILoggingEvent::getLevel)
                .isEqualTo(Level.ERROR);
        } finally {
            detachAppenderFromRolloutServiceLogger(appender);
        }
    }

    @Test
    void testRollOutLoadsEachDeploymentsReferencesInsideItsOwnTransactionAndDeletesDeploymentsWithoutAny() {
        when(projectDeploymentService.getAllProjectDeployments(AUTOMATION_WORKFLOW_PROJECT_ID))
            .thenReturn(List.of(projectDeployment(901L), projectDeployment(902L)));
        when(connectedUserProjectWorkflowRepository.findAllByProjectDeploymentId(901L)).thenReturn(List.of());
        when(connectedUserProjectWorkflowRepository.findAllByProjectDeploymentId(902L))
            .thenReturn(List.of(reference(2L, 902L, SECOND_UUID, false)));

        connectedUserReferenceRolloutManager.rollOut(AUTOMATION_WORKFLOW_PROJECT_ID);

        InOrder inOrder = inOrder(platformTransactionManager, connectedUserProjectWorkflowRepository);

        inOrder.verify(platformTransactionManager)
            .getTransaction(any());
        inOrder.verify(connectedUserProjectWorkflowRepository)
            .findAllByProjectDeploymentId(901L);
        inOrder.verify(platformTransactionManager)
            .getTransaction(any());
        inOrder.verify(connectedUserProjectWorkflowRepository)
            .findAllByProjectDeploymentId(902L);

        verify(connectedUserProjectWorkflowRepository, never()).findAll();
        verify(connectedUserReferenceDeploymentManager, never()).putWorkflows(eq(901L), anyInt(), anyMap());
        verify(connectedUserReferenceDeploymentManager).deleteDeployment(901L);
        verify(connectedUserReferenceDeploymentManager).putWorkflows(eq(902L), eq(2), anyMap());
    }

    @Test
    void testRollOutLogsAnOptimisticLockingFailureAtWarnAndOtherFailuresAtError() {
        when(projectDeploymentService.getAllProjectDeployments(AUTOMATION_WORKFLOW_PROJECT_ID))
            .thenReturn(List.of(projectDeployment(901L), projectDeployment(902L)));
        when(connectedUserProjectWorkflowRepository.findAllByProjectDeploymentId(901L))
            .thenReturn(List.of(reference(1L, 901L, FIRST_UUID, false)));
        when(connectedUserProjectWorkflowRepository.findAllByProjectDeploymentId(902L))
            .thenReturn(List.of(reference(2L, 902L, SECOND_UUID, false)));

        doThrow(new IllegalStateException(
            "Failed to execute DbAction", new OptimisticLockingFailureException("Row was updated concurrently")))
                .when(connectedUserReferenceDeploymentManager)
                .putWorkflows(eq(901L), eq(2), anyMap());
        doThrow(new IllegalStateException("Trigger registration failed"))
            .when(connectedUserReferenceDeploymentManager)
            .putWorkflows(eq(902L), eq(2), anyMap());

        ListAppender<ILoggingEvent> appender = attachAppenderToRolloutServiceLogger();

        try {
            connectedUserReferenceRolloutManager.rollOut(AUTOMATION_WORKFLOW_PROJECT_ID);

            assertThat(appender.list)
                .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
                .containsExactly(
                    tuple(
                        Level.WARN,
                        "Rolling out automation workflow project id=500 to deployment id=901 lost a concurrent update; it will "
                            +
                            "converge on the next publish or enable"),
                    tuple(Level.ERROR, "Rolling out automation workflow project id=500 to deployment id=902 failed"));
        } finally {
            detachAppenderFromRolloutServiceLogger(appender);
        }
    }

    private static ProjectDeployment projectDeployment(long id) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setName("__EMBEDDED__user-" + id + "__PRODUCTION");
        projectDeployment.setProjectId(AUTOMATION_WORKFLOW_PROJECT_ID);
        projectDeployment.setProjectVersion(1);

        return projectDeployment;
    }

    private static ProjectWorkflow projectWorkflow(String uuid) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(AUTOMATION_WORKFLOW_PROJECT_ID, 2, "workflow-" + uuid);

        projectWorkflow.setUuid(uuid);

        return projectWorkflow;
    }

    private static ConnectedUserProjectWorkflow reference(
        long id, long projectDeploymentId, String automationWorkflowUuid, boolean dangling) {

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setAutomationWorkflowUuid(automationWorkflowUuid);
        reference.setConnectedUserProjectId(1L);
        reference.setDangling(dangling);
        reference.setEnabled(!dangling);
        reference.setId(id);
        reference.setProjectDeploymentId(projectDeploymentId);

        return reference;
    }

    private static ListAppender<ILoggingEvent> attachAppenderToRolloutServiceLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(ConnectedUserReferenceRolloutManager.class);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();

        appender.start();
        logger.addAppender(appender);

        return appender;
    }

    private static void detachAppenderFromRolloutServiceLogger(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(ConnectedUserReferenceRolloutManager.class);

        logger.detachAppender(appender);
    }
}
