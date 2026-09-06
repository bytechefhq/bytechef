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

import com.bytechef.component.ComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.filter.ComponentDefinitionFilter;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionServiceTest {

    private static final ComponentHandler TEST_COMPONENT_V1_HANDLER = () -> component("test")
        .title("Test")
        .version(1)
        .actions(action("get").title("Get"));

    private static final ComponentHandler TEST_COMPONENT_V2_HANDLER = () -> component("test")
        .title("Test")
        .version(2)
        .actions(action("get").title("Get"), action("create").title("Create"));

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
    public void testGetComponentDefinitionsReturnsOnlyLatestVersion() {
        ComponentDefinitionService componentDefinitionService = createComponentDefinitionService();

        List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions(
            true, null, null, null, null, PlatformType.AUTOMATION);

        Assertions.assertThat(componentDefinitions)
            .filteredOn(componentDefinition -> "test".equals(componentDefinition.getName()))
            .extracting(ComponentDefinition::getVersion)
            .containsExactly(2);
    }

    @Test
    public void testGetComponentDefinitionsBySearchQueryReturnsOnlyLatestVersion() {
        ComponentDefinitionService componentDefinitionService = createComponentDefinitionService();

        List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions(
            "test", PlatformType.AUTOMATION);

        Assertions.assertThat(componentDefinitions)
            .filteredOn(componentDefinition -> "test".equals(componentDefinition.getName()))
            .extracting(ComponentDefinition::getVersion)
            .containsExactly(2);
    }

    @Test
    public void testGetComponentDefinitionVersionsReturnsAllVersionsInAscendingOrder() {
        ComponentDefinitionService componentDefinitionService = createComponentDefinitionService();

        List<ComponentDefinition> componentDefinitions =
            componentDefinitionService.getComponentDefinitionVersions("test");

        Assertions.assertThat(componentDefinitions)
            .extracting(ComponentDefinition::getVersion)
            .containsExactly(1, 2);
    }

    private static ComponentDefinitionService createComponentDefinitionService() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        component.setRegistry(new ApplicationProperties.Component.Registry());
        applicationProperties.setComponent(component);

        ComponentDefinitionRegistry componentDefinitionRegistry = new ComponentDefinitionRegistry(
            applicationProperties, List.of(TEST_COMPONENT_V2_HANDLER, TEST_COMPONENT_V1_HANDLER), List::of, List.of());

        return new ComponentDefinitionServiceImpl(
            List.of(new AllComponentDefinitionFilter()), componentDefinitionRegistry,
            Mockito.mock(ContextFactory.class));
    }

    private static class AllComponentDefinitionFilter implements ComponentDefinitionFilter {

        @Override
        public boolean filter(ComponentDefinition componentDefinition) {
            return true;
        }

        @Override
        public boolean supports(PlatformType type) {
            return true;
        }
    }
}
