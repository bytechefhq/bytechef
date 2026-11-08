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

package com.bytechef.automation.ai.a2a.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.platform.configuration.domain.Environment;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class A2aServerEnvironmentResolverTest {

    private static final long A2A_SERVER_ID = 9L;

    private final A2aServerRepository a2aServerRepository = mock(A2aServerRepository.class);
    private final A2aServerEnvironmentResolver resolver = new A2aServerEnvironmentResolver(a2aServerRepository);

    @Test
    void testResolvesTheA2aServerResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("A2aServer");
    }

    @Test
    void testReportsTheEnvironmentOfTheServer() {
        A2aServer a2aServer = new A2aServer("server", "description", Environment.STAGING);

        when(a2aServerRepository.findById(A2A_SERVER_ID)).thenReturn(Optional.of(a2aServer));

        assertThat(resolver.fetchEnvironment(A2A_SERVER_ID)).contains(Environment.STAGING);
    }

    @Test
    void testAnUnknownServerHasNoEnvironment() {
        when(a2aServerRepository.findById(A2A_SERVER_ID)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(A2A_SERVER_ID)).isEmpty();
    }

    @Test
    void testANonNumericIdHasNoEnvironment() {
        assertThat(resolver.fetchEnvironment("not-a-number")).isEmpty();

        verifyNoInteractions(a2aServerRepository);
    }

    @Test
    void testTheResolverDependsOnlyOnTheRepositoryAndNeverOnTheGuardedService() {
        Constructor<?>[] constructors = A2aServerEnvironmentResolver.class.getConstructors();

        assertThat(constructors).hasSize(1);

        Class<?>[] parameterTypes = constructors[0].getParameterTypes();

        assertThat(parameterTypes).containsExactly(A2aServerRepository.class);
        assertThat(Arrays.stream(parameterTypes))
            .noneMatch(parameterType -> A2aServerService.class.isAssignableFrom(parameterType));
    }
}
