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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.audit.ProjectDeploymentAuditPublisher;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

/**
 * @author Ivica Cardic
 */
class ProjectDeploymentServiceTest {

    private ProjectDeploymentAuditPublisher projectDeploymentAuditPublisher;
    private ProjectDeploymentRepository projectDeploymentRepository;
    private ProjectDeploymentService projectDeploymentService;

    @BeforeEach
    void beforeEach() {
        projectDeploymentAuditPublisher = mock(ProjectDeploymentAuditPublisher.class);
        projectDeploymentRepository = mock(ProjectDeploymentRepository.class);
        projectDeploymentService = new ProjectDeploymentServiceImpl(
            projectDeploymentAuditPublisher, projectDeploymentRepository);
    }

    @Test
    void testCreateAssignsUuidWhenMissing() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName("d");
        projectDeployment.setProjectId(1L);
        projectDeployment.setUuid(null);

        when(projectDeploymentRepository.save(any())).thenAnswer(this::echoWithGeneratedId);

        ProjectDeployment created = projectDeploymentService.create(projectDeployment);

        assertThat(created.getUuid()).isNotNull();
    }

    @Test
    void testCreateKeepsProvidedUuid() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName("d");
        projectDeployment.setProjectId(1L);

        UUID providedUuid = UUID.randomUUID();

        projectDeployment.setUuid(providedUuid);

        when(projectDeploymentRepository.save(any())).thenAnswer(this::echoWithGeneratedId);

        ProjectDeployment created = projectDeploymentService.create(projectDeployment);

        assertThat(created.getUuid()).isEqualTo(providedUuid);
    }

    @Test
    void testUpdateDoesNotChangeUuidOrEnvironment() {
        ProjectDeployment current = new ProjectDeployment();

        current.setId(1L);
        current.setName("d");
        current.setProjectId(1L);
        current.setEnvironment(Environment.DEVELOPMENT);

        UUID currentUuid = UUID.randomUUID();

        current.setUuid(currentUuid);

        when(projectDeploymentRepository.findById(1L)).thenReturn(Optional.of(current));
        when(projectDeploymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDeployment incoming = new ProjectDeployment();

        incoming.setId(1L);
        incoming.setName("renamed");
        incoming.setProjectId(1L);
        incoming.setEnvironment(Environment.PRODUCTION);
        incoming.setUuid(UUID.randomUUID());

        ProjectDeployment updated = projectDeploymentService.update(incoming);

        assertThat(updated.getUuid()).isEqualTo(currentUuid);
        assertThat(updated.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(updated.getName()).isEqualTo("renamed");
    }

    @Test
    void testFetchProjectDeploymentById() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        when(projectDeploymentRepository.findById(1L)).thenReturn(Optional.of(projectDeployment));

        assertThat(projectDeploymentService.fetchProjectDeployment(1L)).contains(projectDeployment);
    }

    @Test
    void testFetchProjectDeploymentByIdIsEmptyWhenMissing() {
        when(projectDeploymentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(projectDeploymentService.fetchProjectDeployment(404L)).isEmpty();
    }

    @Test
    void testFetchProjectDeploymentByUuidAndEnvironment() {
        UUID uuid = UUID.randomUUID();
        ProjectDeployment projectDeployment = new ProjectDeployment();

        when(projectDeploymentRepository.findByUuidAndEnvironment(uuid, Environment.STAGING.ordinal()))
            .thenReturn(Optional.of(projectDeployment));

        assertThat(projectDeploymentService.fetchProjectDeployment(uuid, Environment.STAGING))
            .contains(projectDeployment);
    }

    private ProjectDeployment echoWithGeneratedId(InvocationOnMock invocation) {
        ProjectDeployment savedProjectDeployment = invocation.getArgument(0);

        savedProjectDeployment.setId(1L);

        return savedProjectDeployment;
    }
}
