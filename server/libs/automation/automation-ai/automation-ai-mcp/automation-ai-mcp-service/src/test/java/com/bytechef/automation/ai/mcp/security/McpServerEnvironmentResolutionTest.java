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

package com.bytechef.automation.ai.mcp.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpServerService;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * A non-admin by-id check on {@code 'McpServer'} resolves the server's environment through
 * {@link McpServerEnvironmentResolver}. The guarded {@code McpServerService.getMcpServer} runs that same check, so a
 * resolver reading through the service recurses on every check; the resolver therefore depends on the repository only.
 *
 * @author Ivica Cardic
 */
class McpServerEnvironmentResolutionTest {

    @Test
    void testTheResolverDependsOnlyOnTheRepositoryAndNeverOnTheGuardedService() {
        Constructor<?>[] constructors = McpServerEnvironmentResolver.class.getConstructors();

        assertThat(constructors).hasSize(1);

        Class<?>[] parameterTypes = constructors[0].getParameterTypes();

        assertThat(parameterTypes).containsExactly(McpServerRepository.class);
        assertThat(Arrays.stream(parameterTypes))
            .noneMatch(parameterType -> McpServerService.class.isAssignableFrom(parameterType));
    }
}
