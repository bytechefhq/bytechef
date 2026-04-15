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

package com.bytechef.automation.configuration.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.dto.BulkReassignResultDTO;
import com.bytechef.automation.configuration.event.WorkspaceUserRemovedEvent;
import com.bytechef.automation.configuration.facade.ConnectionReassignmentFacade;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceUserRemovalListenerTest {

    @Mock
    private ConnectionReassignmentFacade connectionReassignmentFacade;

    @Mock
    private ObjectProvider<MeterRegistry> meterRegistryProvider;

    @InjectMocks
    private WorkspaceUserRemovalListener listener;

    @Test
    void testOnWorkspaceUserRemovedDelegatesToFacade() {
        WorkspaceUserRemovedEvent event = new WorkspaceUserRemovedEvent(42L, "removed@example.com");

        when(connectionReassignmentFacade.markConnectionsPendingReassignment(42L, "removed@example.com"))
            .thenReturn(new BulkReassignResultDTO(0, 0, 0, 0, java.util.List.of()));

        listener.onWorkspaceUserRemoved(event);

        verify(connectionReassignmentFacade).markConnectionsPendingReassignment(42L, "removed@example.com");
    }

    @Test
    void testOnWorkspaceUserRemovedSwallowsFacadeExceptionSoOuterTransactionStaysCommitted() {
        WorkspaceUserRemovedEvent event = new WorkspaceUserRemovedEvent(42L, "removed@example.com");

        doThrow(new RuntimeException("transient DB failure"))
            .when(connectionReassignmentFacade)
            .markConnectionsPendingReassignment(42L, "removed@example.com");

        assertThatCode(() -> listener.onWorkspaceUserRemoved(event)).doesNotThrowAnyException();

        verify(connectionReassignmentFacade).markConnectionsPendingReassignment(42L, "removed@example.com");
    }
}
