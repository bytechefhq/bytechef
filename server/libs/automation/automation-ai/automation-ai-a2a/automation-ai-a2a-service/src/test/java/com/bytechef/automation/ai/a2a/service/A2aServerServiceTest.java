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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.prepost.PreAuthorize;

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
    void testCreatePersistsTheGivenFields() {
        when(a2aServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        a2aServerService.create(new A2aServer("agent", "answers questions", Environment.DEVELOPMENT));

        ArgumentCaptor<A2aServer> a2aServerArgumentCaptor = ArgumentCaptor.forClass(A2aServer.class);

        verify(a2aServerRepository).save(a2aServerArgumentCaptor.capture());

        A2aServer savedA2aServer = a2aServerArgumentCaptor.getValue();

        assertThat(savedA2aServer.getName()).isEqualTo("agent");
        assertThat(savedA2aServer.getDescription()).isEqualTo("answers questions");
        assertThat(savedA2aServer.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(savedA2aServer.getUuid()).isNotNull();
    }

    @Test
    void testCreateAssignsUuidWhenExplicitlyNull() {
        A2aServer a2aServer = new A2aServer("agent", null, Environment.DEVELOPMENT);

        a2aServer.setUuid(null);

        when(a2aServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        A2aServer created = a2aServerService.create(a2aServer);

        assertThat(created.getUuid()).isNotNull();
    }

    @Test
    void testUpdateKeepsUuid() {
        A2aServer current = new A2aServer("agent", null, Environment.DEVELOPMENT);
        String currentSecretKey = current.getSecretKey();
        UUID currentUuid = current.getUuid();

        current.setId(3L);

        when(a2aServerRepository.findById(3L)).thenReturn(Optional.of(current));
        when(a2aServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        A2aServer incoming = new A2aServer("agent2", null, Environment.DEVELOPMENT);

        incoming.setId(3L);
        incoming.setUuid(UUID.randomUUID());

        A2aServer updated = a2aServerService.update(incoming);

        assertThat(updated.getUuid()).isEqualTo(currentUuid);
        assertThat(updated.getSecretKey()).isEqualTo(currentSecretKey)
            .isNotEqualTo(incoming.getSecretKey());
    }

    @Test
    void testUpdateKeepsTheEnvironment() {
        A2aServer current = new A2aServer("agent", null, Environment.DEVELOPMENT);

        current.setId(3L);

        when(a2aServerRepository.findById(3L)).thenReturn(Optional.of(current));
        when(a2aServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        A2aServer incoming = new A2aServer("agent", null, Environment.PRODUCTION);

        incoming.setId(3L);

        assertThat(a2aServerService.update(incoming)
            .getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
    }

    @Test
    void testFetchBySecretKeyIsEmptyForAnUnknownKey() {
        when(a2aServerRepository.findBySecretKey("unknown")).thenReturn(Optional.empty());

        assertThat(a2aServerService.fetchA2aServer("unknown")).isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> a2aServerService.getA2aServer("unknown"));
    }

    @Test
    void testCreateRejectsAServerWithoutASecretKey() {
        assertThatNullPointerException()
            .isThrownBy(() -> a2aServerService.create(new A2aServer()))
            .withMessageContaining("secret key");

        verify(a2aServerRepository, never()).save(any());
    }

    @Test
    void testGetA2aServerSecretKeyReturnsTheSecretKey() {
        A2aServer a2aServer = new A2aServer("agent", null, Environment.PRODUCTION);

        when(a2aServerRepository.findById(5L)).thenReturn(Optional.of(a2aServer));

        assertThat(a2aServerService.getA2aServerSecretKey(5L)).isEqualTo(a2aServer.getSecretKey());
    }

    @Test
    void testGetA2aServerSecretKeyRequiresTenantAdmin() throws NoSuchMethodException {
        Method method = A2aServerServiceImpl.class.getMethod("getA2aServerSecretKey", long.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("isTenantAdmin()");
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void testGetA2aServerBySecretKeyDoesNotDiscloseTheKeyWhenMissing() {
        when(a2aServerRepository.findBySecretKey("server-secret")).thenReturn(Optional.empty());

        assertThatIllegalArgumentException()
            .isThrownBy(() -> a2aServerService.getA2aServer("server-secret"))
            .withMessageNotContaining("server-secret");
    }
}
