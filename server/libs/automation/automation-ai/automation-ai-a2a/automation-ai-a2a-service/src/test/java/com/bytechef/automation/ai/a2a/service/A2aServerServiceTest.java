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

package com.bytechef.automation.ai.a2a.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class A2aServerServiceTest {

    private A2aServerRepository a2aServerRepository;
    private A2aServerService a2aServerService;

    @BeforeEach
    void beforeEach() {
        a2aServerRepository = mock(A2aServerRepository.class);
        a2aServerService = new A2aServerServiceImpl(a2aServerRepository);
    }

    @Test
    void testCreateAssignsUuidWhenMissing() {
        when(a2aServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        A2aServer created = a2aServerService.create("agent", null, PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        assertThat(created.getUuid()).isNotNull();
    }

    @Test
    void testCreateAssignsUuidWhenExplicitlyNull() {
        A2aServer a2aServer = new A2aServer("agent", null, PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        a2aServer.setUuid(null);

        when(a2aServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        A2aServer created = a2aServerService.create(a2aServer);

        assertThat(created.getUuid()).isNotNull();
    }

    @Test
    void testUpdateKeepsUuid() {
        A2aServer current = new A2aServer("agent", null, PlatformType.AUTOMATION, Environment.DEVELOPMENT);
        UUID currentUuid = current.getUuid();

        current.setId(3L);

        when(a2aServerRepository.findById(3L)).thenReturn(Optional.of(current));
        when(a2aServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        A2aServer incoming = new A2aServer("agent2", null, PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        incoming.setId(3L);
        incoming.setUuid(UUID.randomUUID());

        assertThat(a2aServerService.update(incoming)
            .getUuid()).isEqualTo(currentUuid);
    }

    @Test
    void testFetchByUuidAndEnvironment() {
        UUID uuid = UUID.randomUUID();
        A2aServer a2aServer = new A2aServer("agent", null, PlatformType.AUTOMATION, Environment.PRODUCTION);

        when(a2aServerRepository.findByUuidAndEnvironment(uuid, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.of(a2aServer));

        assertThat(a2aServerService.fetchA2aServer(uuid, Environment.PRODUCTION)).contains(a2aServer);
    }
}
