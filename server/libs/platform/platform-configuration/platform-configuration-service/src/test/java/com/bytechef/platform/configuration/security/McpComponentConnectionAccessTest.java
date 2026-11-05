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

package com.bytechef.platform.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * @author Ivica Cardic
 */
class McpComponentConnectionAccessTest {

    @Test
    void testIsMcpComponentConnectionDeniesWithoutAResolver() {
        McpComponentConnectionAccess mcpComponentConnectionAccess = new McpComponentConnectionAccess(
            new StaticListableBeanFactory().getBeanProvider(McpComponentConnectionAccessResolver.class));

        assertThat(mcpComponentConnectionAccess.isMcpComponentConnection(1L, "slack", 1, 2L)).isFalse();
    }

    @Test
    void testIsMcpComponentConnectionGrantsWhenAResolverGrants() {
        StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory();

        staticListableBeanFactory.addBean(
            "denyingResolver",
            (McpComponentConnectionAccessResolver) (
                mcpComponentId, componentName, componentVersion,
                connectionId) -> false);
        staticListableBeanFactory.addBean(
            "grantingResolver",
            (McpComponentConnectionAccessResolver) (
                mcpComponentId, componentName, componentVersion,
                connectionId) -> mcpComponentId == 1L && connectionId == 2L);

        McpComponentConnectionAccess mcpComponentConnectionAccess = new McpComponentConnectionAccess(
            staticListableBeanFactory.getBeanProvider(McpComponentConnectionAccessResolver.class));

        assertThat(mcpComponentConnectionAccess.isMcpComponentConnection(1L, "slack", 1, 2L)).isTrue();
        assertThat(mcpComponentConnectionAccess.isMcpComponentConnection(1L, "slack", 1, 3L)).isFalse();
    }
}
