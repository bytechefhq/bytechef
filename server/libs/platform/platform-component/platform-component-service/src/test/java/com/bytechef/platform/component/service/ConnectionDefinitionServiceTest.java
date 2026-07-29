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

package com.bytechef.platform.component.service;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.aspect.TokenRefreshHandler;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ScriptComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class ConnectionDefinitionServiceTest {

    private final ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
    private final ContextFactory contextFactory = mock(ContextFactory.class);
    private final TokenRefreshHandler tokenRefreshHandler = mock(TokenRefreshHandler.class);

    private final ConnectionDefinitionServiceImpl connectionDefinitionService = new ConnectionDefinitionServiceImpl(
        componentDefinitionRegistry, contextFactory, tokenRefreshHandler);

    @Test
    void testGetConnectionDefinitionsForScriptComponentUsesStubEnumeration() {
        ScriptComponentDefinition scriptComponentDefinition = mock(ScriptComponentDefinition.class);

        ComponentDefinition connectableComponentDefinition = component("slack")
            .version(1)
            .title("Slack")
            .connection(connection().version(3))
            .actions(action("sendMessage"));

        when(componentDefinitionRegistry.getComponentDefinition("script", 1)).thenReturn(scriptComponentDefinition);
        when(componentDefinitionRegistry.getStaticComponentDefinitions())
            .thenReturn(List.of(connectableComponentDefinition));

        List<ConnectionDefinition> connectionDefinitions =
            connectionDefinitionService.getConnectionDefinitions("script", 1);

        // The connectable-components list must come from index stubs, never the full catalog...
        verify(componentDefinitionRegistry).getStaticComponentDefinitions();
        verify(componentDefinitionRegistry, never()).getComponentDefinitions();

        // ...and toConnectionDefinition must build cleanly from a summary-only stub connection (identity + version).
        assertThat(connectionDefinitions).singleElement()
            .satisfies(connectionDefinition -> {
                assertThat(connectionDefinition.getComponentName()).isEqualTo("slack");
                assertThat(connectionDefinition.getVersion()).isEqualTo(3);
            });
    }

    @Test
    void testGetConnectionDefinitionsUsesStubEnumeration() {
        ComponentDefinition connectableComponentDefinition = component("slack")
            .version(1)
            .title("Slack")
            .connection(connection().version(3))
            .actions(action("sendMessage"));

        when(componentDefinitionRegistry.getStaticComponentDefinitions())
            .thenReturn(List.of(connectableComponentDefinition));

        List<ConnectionDefinition> connectionDefinitions = connectionDefinitionService.getConnectionDefinitions();

        // The no-arg connections list must come from index stubs, never the full catalog.
        verify(componentDefinitionRegistry).getStaticComponentDefinitions();
        verify(componentDefinitionRegistry, never()).getComponentDefinitions();

        assertThat(connectionDefinitions).singleElement()
            .satisfies(connectionDefinition -> {
                assertThat(connectionDefinition.getComponentName()).isEqualTo("slack");
                assertThat(connectionDefinition.getVersion()).isEqualTo(3);
            });
    }

    @Disabled
    @Test
    public void testConnectionExists() {
// TODO
    }

    @Disabled
    @Test
    public void testPerformAuthorizationApply() {
// TODO
    }

    @Disabled
    @Test
    public void testPerformAuthorizationCallback() {
// TODO
    }

    @Disabled
    @Test
    public void testFetchBaseUri() {
// TODO
    }

    @Disabled
    @Test
    public void testGetAuthorizationType() {
// TODO
    }

    @Disabled
    @Test
    public void testGetConnectionDefinition() {
// TODO
    }

    @Disabled
    @Test
    public void testGetConnectionDefinitions() {
// TODO
    }

    @Disabled
    @Test
    public void testGetOAuth2Parameters() {
        // TODO
    }
}
