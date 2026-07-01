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

package com.bytechef.platform.component;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.Property;
import com.bytechef.component.slack.SlackComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ComponentDefinitionRegistryTest {

    private static ComponentDefinitionRegistry createRegistry() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        component.setRegistry(new ApplicationProperties.Component.Registry());
        applicationProperties.setComponent(component);

        return new ComponentDefinitionRegistry(
            applicationProperties,
            List.of(new SlackComponentHandler()),
            List::of,
            List.of());
    }

    @Disabled
    @Test
    public void testGetActionDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetActionDefinitions() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetActionProperty() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetAuthorization() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetComponentConnectionDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetComponentDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetComponentDefinitions() {
        // TODO
    }

    @Test
    public void testGetComponentInputProperty() throws Exception {
        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry();

        Property property = componentDefinitionRegistry.getComponentInputProperty(
            "slack", 1, "channel", "channel", ParametersFactory.create(Map.of()),
            ParametersFactory.create((ComponentConnection) null), Map.of(), Mockito.mock(ActionContext.class));

        Assertions.assertThat(property.getName())
            .isEqualTo("channel");
        Assertions.assertThat(property)
            .isInstanceOf(DynamicOptionsProperty.class);
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
    public void testGetTriggerDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetTriggerDefinitions() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetTriggerProperty() {
        // TODO
    }
}
