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

package com.bytechef.platform.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Nikolina Spehar
 * @author Igor Beslic
 */
@ExtendWith(MockitoExtension.class)
public class ConnectionLifecycleFacadeTest {

    @Mock
    private ConnectionRefreshScheduler connectionRefreshScheduler;

    @Captor
    private ArgumentCaptor<Long> connectionIdCaptor;

    @Captor
    private ArgumentCaptor<Instant> expiryCaptor;

    @Captor
    private ArgumentCaptor<String> tenantIdCaptor;

    private ConnectionLifecycleFacadeImpl connectionLifecycleFacade;

    @BeforeEach
    void setUp() {
        connectionLifecycleFacade = new ConnectionLifecycleFacadeImpl(connectionRefreshScheduler);
    }

    @Test
    public void testScheduleConnectionRefresh() {
        Long connectionId = 42L;
        String tenantId = "tenant-1";
        int expiresIn = 3600;
        Map<String, Object> parameters = Map.of("expires_in", expiresIn);

        Instant beforeCall = Instant.now();

        connectionLifecycleFacade.scheduleConnectionRefresh(
            connectionId, parameters, AuthorizationType.OAUTH2_AUTHORIZATION_CODE, tenantId);

        Instant afterCall = Instant.now();

        verify(connectionRefreshScheduler).scheduleConnectionRefresh(
            connectionIdCaptor.capture(), expiryCaptor.capture(), tenantIdCaptor.capture());

        assertThat(connectionIdCaptor.getValue()).isEqualTo(connectionId);
        assertThat(tenantIdCaptor.getValue()).isEqualTo(tenantId);
        assertThat(expiryCaptor.getValue())
            .isBetween(beforeCall.plusSeconds(expiresIn), afterCall.plusSeconds(expiresIn));
    }

    @Disabled
    @Test
    public void testDeleteScheduledConnectionRefresh() {
        // TODO
    }
}
