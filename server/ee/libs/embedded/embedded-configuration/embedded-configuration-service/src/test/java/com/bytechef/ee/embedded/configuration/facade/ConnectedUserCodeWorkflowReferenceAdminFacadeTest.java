/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserCodeWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserCodeWorkflowReferenceAdminFacadeTest {

    @Mock
    private ConnectedUserProjectService connectedUserProjectService;

    @Mock
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Mock
    private ConnectedUserService connectedUserService;

    @InjectMocks
    private ConnectedUserCodeWorkflowReferenceAdminFacadeImpl connectedUserCodeWorkflowReferenceAdminFacade;

    @Test
    void testGetReferencesJoinsBackToTheOwningConnectedUserInBatch() {
        ConnectedUserProjectWorkflow reference1 = new ConnectedUserProjectWorkflow();

        reference1.setConnectedUserProjectId(1L);
        reference1.setCatalogWorkflowUuid("uuid-1");
        reference1.setEnabled(true);
        reference1.setDangling(false);

        ConnectedUserProjectWorkflow reference2 = new ConnectedUserProjectWorkflow();

        reference2.setConnectedUserProjectId(3L);
        reference2.setCatalogWorkflowUuid("uuid-2");
        reference2.setEnabled(false);
        reference2.setDangling(true);
        reference2.setDanglingReason("catalog workflow removed");

        when(connectedUserProjectWorkflowRepository.findAllByCatalogWorkflowUuidIn(Set.of("uuid-1", "uuid-2")))
            .thenReturn(List.of(reference1, reference2));

        ConnectedUserProject connectedUserProject1 = new ConnectedUserProject();

        connectedUserProject1.setId(1L);
        connectedUserProject1.setConnectedUserId(2L);

        ConnectedUserProject connectedUserProject2 = new ConnectedUserProject();

        connectedUserProject2.setId(3L);
        connectedUserProject2.setConnectedUserId(4L);

        when(connectedUserProjectService.getConnectedUserProjects(anyList()))
            .thenReturn(List.of(connectedUserProject1, connectedUserProject2));

        ConnectedUser connectedUser1 = new ConnectedUser(Map.of(), null, true, "ext-1", 2L, null, 0);
        ConnectedUser connectedUser2 = new ConnectedUser(Map.of(), null, true, "ext-2", 4L, null, 0);

        when(connectedUserService.getConnectedUsers(anyList()))
            .thenReturn(List.of(connectedUser1, connectedUser2));

        List<ConnectedUserCodeWorkflowReferenceDTO> result =
            connectedUserCodeWorkflowReferenceAdminFacade.getReferences(Set.of("uuid-1", "uuid-2"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)
            .externalUserId()).isEqualTo("ext-1");
        assertThat(result.get(1)
            .externalUserId()).isEqualTo("ext-2");

        verify(connectedUserProjectService, times(1)).getConnectedUserProjects(anyList());
        verify(connectedUserService, times(1)).getConnectedUsers(anyList());
    }
}
