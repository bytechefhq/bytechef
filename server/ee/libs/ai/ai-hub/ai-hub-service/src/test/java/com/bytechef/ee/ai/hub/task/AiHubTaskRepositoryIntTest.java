/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.task.repository.AiHubTaskRepository;
import com.bytechef.ee.ai.hub.task.repository.WorkspaceAiHubTaskRepository;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link AiHubTaskRepository}. Workspace dimension comes from {@code workspace_ai_hub_task} via
 * the JOIN-based queries; the entity itself is workspace-agnostic.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiHubTaskRepositoryIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubTaskRepositoryIntTest {

    @Autowired
    private AiHubTaskRepository taskRepository;

    @Autowired
    private WorkspaceAiHubTaskRepository workspaceTaskRepository;

    @AfterEach
    public void afterEach() {
        workspaceTaskRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @Test
    public void testSaveAndFindById() {
        long taskId = saveTaskWithMembership(1L, 10L, "thread-1", AiHubTaskStatus.ACTIVE);

        Optional<AiHubTask> found = taskRepository.findById(taskId);

        assertThat(found).isPresent();
        assertThat(found.get()
            .getThreadId()).isEqualTo("thread-1");
        assertThat(found.get()
            .getStatus()).isEqualTo(AiHubTaskStatus.ACTIVE);
    }

    @Test
    public void testFindByThreadIdAndUserId() {
        saveTaskWithMembership(1L, 10L, "thread-abc", AiHubTaskStatus.ACTIVE);

        Optional<AiHubTask> found = taskRepository.findByThreadIdAndUserId("thread-abc", 10L);

        assertThat(found).isPresent();

        Optional<AiHubTask> notFound = taskRepository.findByThreadIdAndUserId("thread-abc", 99L);

        assertThat(notFound).isEmpty();
    }

    @Test
    public void testFindByWorkspaceIdAndUserIdAndStatusOrderByUpdatedAtDesc() {
        AiHubTask active1 = buildTask(10L, "thread-a1", AiHubTaskStatus.ACTIVE);

        active1.setUpdatedAt(LocalDateTime.now()
            .minusMinutes(5));

        AiHubTask active2 = buildTask(10L, "thread-a2", AiHubTaskStatus.ACTIVE);

        active2.setUpdatedAt(LocalDateTime.now());

        AiHubTask archived = buildTask(10L, "thread-arch", AiHubTaskStatus.ARCHIVED);

        saveWithMembership(active1, 1L);
        saveWithMembership(active2, 1L);
        saveWithMembership(archived, 1L);

        List<AiHubTask> activeTasks = taskRepository
            .findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(1L, 10L, 0,
                AiHubTaskStatus.ACTIVE.ordinal(),
                100);

        assertThat(activeTasks).hasSize(2);
        assertThat(activeTasks.get(0)
            .getThreadId()).isEqualTo("thread-a2");
        assertThat(activeTasks.get(1)
            .getThreadId()).isEqualTo("thread-a1");
    }

    @Test
    public void testKindColumnDefaultsToStandardForLegacyRows() {
        AiHubTask task = new AiHubTask();

        task.setUserId(10L);
        task.setThreadId("legacy-thread");
        task.setStatus(AiHubTaskStatus.ACTIVE);
        task.setMessageCount(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        long taskId = saveWithMembership(task, 1L);

        AiHubTask reloaded = taskRepository.findById(taskId)
            .orElseThrow();

        assertThat(reloaded.getKind()).isEqualTo(AiHubTaskKind.STANDARD);
        assertThat(reloaded.getWorkflowExecutionId()).isNull();
        assertThat(reloaded.getProjectDeploymentId()).isNull();
    }

    @Test
    public void testFindByWorkspaceIdAndUserIdAndStatusFiltersOtherUsers() {
        saveWithMembership(buildTask(10L, "thread-user10", AiHubTaskStatus.ACTIVE), 1L);
        saveWithMembership(buildTask(20L, "thread-user20", AiHubTaskStatus.ACTIVE), 1L);

        List<AiHubTask> tasks = taskRepository
            .findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(1L, 10L, 0,
                AiHubTaskStatus.ACTIVE.ordinal(),
                100);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0)
            .getUserId()).isEqualTo(10L);
    }

    private long saveTaskWithMembership(
        long workspaceId, long userId, String threadId, AiHubTaskStatus status) {
        return saveWithMembership(buildTask(userId, threadId, status), workspaceId);
    }

    private long saveWithMembership(AiHubTask task, long workspaceId) {
        AiHubTask saved = taskRepository.save(task);

        workspaceTaskRepository.save(new WorkspaceAiHubTask(workspaceId, saved.getId()));

        return saved.getId();
    }

    private static AiHubTask buildTask(
        long userId, String threadId, AiHubTaskStatus status) {

        AiHubTask task = new AiHubTask();

        task.setUserId(userId);
        task.setThreadId(threadId);
        task.setStatus(status);
        task.setMessageCount(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        return task;
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class,
        AiHubTaskRepositoryIntTest.IntTestConfiguration.IntTestJdbcConfiguration.class
    })
    @Configuration
    public static class IntTestConfiguration {

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        @Configuration
        public static class IntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
