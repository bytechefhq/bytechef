/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.event.ProjectCreatedEvent;
import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Unit tests for {@link ProjectCreatedEventListener}. Guards the two load-bearing behaviors documented in the
 * listener's Javadoc:
 * <ol>
 * <li>On the happy path, the creator is persisted as project ADMIN.</li>
 * <li>If persistence fails, the exception propagates so the enclosing project-creation transaction rolls back.
 * Swallowing the exception would silently orphan the project.</li>
 * </ol>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectCreatedEventListenerTest {

    private static final long PROJECT_ID = 100L;
    private static final long CREATOR_USER_ID = 42L;

    private ProjectUserRepository projectUserRepository;
    private ProjectCreatedEventListener listener;

    @BeforeEach
    void setUp() {
        projectUserRepository = mock(ProjectUserRepository.class);
        listener = new ProjectCreatedEventListener(projectUserRepository);

        when(projectUserRepository.save(any(ProjectUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testOnProjectCreatedPersistsCreatorAsAdmin() {
        listener.onProjectCreated(new ProjectCreatedEvent(PROJECT_ID, CREATOR_USER_ID));

        ArgumentCaptor<ProjectUser> captor = ArgumentCaptor.forClass(ProjectUser.class);

        verify(projectUserRepository).save(captor.capture());

        ProjectUser saved = captor.getValue();

        assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getUserId()).isEqualTo(CREATOR_USER_ID);
        assertThat(saved.getProjectRole()).isEqualTo(ProjectRole.ADMIN.ordinal());
        assertThat(saved.getCustomRoleId()).isNull();
    }

    @Test
    void testOnProjectCreatedPropagatesPersistenceFailureForRollback() {
        // Load-bearing: the listener MUST re-throw so the enclosing @Transactional project creation rolls back.
        // Swallowing would commit the project without an admin row — silently orphaning the project.
        DataIntegrityViolationException saveFailure = new DataIntegrityViolationException("unique constraint");

        doThrow(saveFailure)
            .when(projectUserRepository)
            .save(any(ProjectUser.class));

        assertThatThrownBy(() -> listener.onProjectCreated(new ProjectCreatedEvent(PROJECT_ID, CREATOR_USER_ID)))
            .isSameAs(saveFailure);
    }
}
