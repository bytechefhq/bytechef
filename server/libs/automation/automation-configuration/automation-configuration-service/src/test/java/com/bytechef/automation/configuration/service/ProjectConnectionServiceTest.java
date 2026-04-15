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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectConnection;
import com.bytechef.automation.configuration.repository.ProjectConnectionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectConnectionServiceTest {

    private static final long CONNECTION_ID_1 = 101L;
    private static final long CONNECTION_ID_2 = 102L;
    private static final long PROJECT_ID_1 = 201L;
    private static final long PROJECT_ID_2 = 202L;

    @Mock
    private ProjectConnectionRepository projectConnectionRepository;

    private ProjectConnectionService projectConnectionService;

    @BeforeEach
    void beforeEach() {
        projectConnectionService = new ProjectConnectionServiceImpl(projectConnectionRepository);
    }

    @Test
    void testCreate() {
        ProjectConnection saved = new ProjectConnection(CONNECTION_ID_1, PROJECT_ID_1);

        when(projectConnectionRepository.save(any(ProjectConnection.class))).thenReturn(saved);

        ProjectConnection projectConnection = projectConnectionService.create(CONNECTION_ID_1, PROJECT_ID_1);

        assertThat(projectConnection.getConnectionId()).isEqualTo(CONNECTION_ID_1);
        assertThat(projectConnection.getProjectId()).isEqualTo(PROJECT_ID_1);

        verify(projectConnectionRepository).save(any(ProjectConnection.class));
    }

    @Test
    void testDelete() {
        projectConnectionService.delete(CONNECTION_ID_1, PROJECT_ID_1);

        verify(projectConnectionRepository).deleteByConnectionIdAndProjectId(CONNECTION_ID_1, PROJECT_ID_1);
    }

    @Test
    void testGetProjectConnections() {
        ProjectConnection pc1 = new ProjectConnection(CONNECTION_ID_1, PROJECT_ID_1);
        ProjectConnection pc2 = new ProjectConnection(CONNECTION_ID_2, PROJECT_ID_1);

        when(projectConnectionRepository.findAllByProjectId(PROJECT_ID_1)).thenReturn(List.of(pc1, pc2));

        List<ProjectConnection> projectConnections = projectConnectionService.getProjectConnections(PROJECT_ID_1);

        assertThat(projectConnections).hasSize(2);
        assertThat(projectConnections)
            .extracting(ProjectConnection::getConnectionId)
            .containsExactlyInAnyOrder(CONNECTION_ID_1, CONNECTION_ID_2);
    }

    @Test
    void testGetProjectConnectionsByProjectIds() {
        ProjectConnection pc1 = new ProjectConnection(CONNECTION_ID_1, PROJECT_ID_1);
        ProjectConnection pc2 = new ProjectConnection(CONNECTION_ID_2, PROJECT_ID_2);

        when(projectConnectionRepository.findAllByProjectIdIn(List.of(PROJECT_ID_1, PROJECT_ID_2)))
            .thenReturn(List.of(pc1, pc2));

        List<ProjectConnection> projectConnections =
            projectConnectionService.getProjectConnectionsByProjectIds(List.of(PROJECT_ID_1, PROJECT_ID_2));

        assertThat(projectConnections).hasSize(2);
        assertThat(projectConnections)
            .extracting(ProjectConnection::getProjectId)
            .containsExactlyInAnyOrder(PROJECT_ID_1, PROJECT_ID_2);
    }

    @Test
    void testGetConnectionProjects() {
        ProjectConnection pc1 = new ProjectConnection(CONNECTION_ID_1, PROJECT_ID_1);
        ProjectConnection pc2 = new ProjectConnection(CONNECTION_ID_1, PROJECT_ID_2);

        when(projectConnectionRepository.findAllByConnectionId(CONNECTION_ID_1)).thenReturn(List.of(pc1, pc2));

        List<ProjectConnection> connectionProjects = projectConnectionService.getConnectionProjects(CONNECTION_ID_1);

        assertThat(connectionProjects).hasSize(2);
        assertThat(connectionProjects)
            .extracting(ProjectConnection::getProjectId)
            .containsExactlyInAnyOrder(PROJECT_ID_1, PROJECT_ID_2);
    }

    @Test
    void testDeleteByConnectionId() {
        projectConnectionService.deleteByConnectionId(CONNECTION_ID_1);

        verify(projectConnectionRepository).deleteAllByConnectionId(CONNECTION_ID_1);
    }
}
