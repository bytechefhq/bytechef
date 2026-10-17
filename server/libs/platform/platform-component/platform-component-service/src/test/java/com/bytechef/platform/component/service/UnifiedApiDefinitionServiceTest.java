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
import static com.bytechef.component.definition.ComponentDsl.unifiedApi;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.domain.ComponentDefinition;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class UnifiedApiDefinitionServiceTest {

    private static final ComponentHandler ACCOUNTING_COMPONENT_HANDLER = () -> component("accountingTest")
        .title("Accounting Test")
        .actions(action("get").title("Get"))
        .unifiedApi(unifiedApi(UnifiedApiCategory.ACCOUNTING));

    private static final ComponentHandler CRM_COMPONENT_HANDLER = () -> component("crmTest")
        .title("CRM Test")
        .actions(action("get").title("Get"))
        .unifiedApi(unifiedApi(UnifiedApiCategory.CRM));

    private static final ComponentHandler PLAIN_COMPONENT_HANDLER = () -> component("plainTest")
        .title("Plain Test")
        .actions(action("get").title("Get"));

    private static final ComponentHandler TICKETING_COMPONENT_HANDLER = () -> component("ticketingTest")
        .title("Ticketing Test")
        .actions(action("get").title("Get"))
        .unifiedApi(unifiedApi(UnifiedApiCategory.TICKETING));

    @Test
    public void testGetUnifiedApiComponentDefinitionsReturnsOnlyRequestedCategory() {
        UnifiedApiDefinitionService unifiedApiDefinitionService = createUnifiedApiDefinitionService();

        List<ComponentDefinition> componentDefinitions =
            unifiedApiDefinitionService.getUnifiedApiComponentDefinitions(UnifiedApiCategory.CRM);

        Assertions.assertThat(componentDefinitions)
            .extracting(ComponentDefinition::getName)
            .containsExactly("crmTest");
    }

    @Test
    public void testGetUnifiedApiComponentDefinitionsReturnsEmptyForCategoryWithoutComponents() {
        UnifiedApiDefinitionService unifiedApiDefinitionService = createUnifiedApiDefinitionService();

        List<ComponentDefinition> componentDefinitions =
            unifiedApiDefinitionService.getUnifiedApiComponentDefinitions(UnifiedApiCategory.HRIS);

        Assertions.assertThat(componentDefinitions)
            .isEmpty();
    }

    private static UnifiedApiDefinitionService createUnifiedApiDefinitionService() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        component.setRegistry(new ApplicationProperties.Component.Registry());
        applicationProperties.setComponent(component);

        ComponentDefinitionRegistry componentDefinitionRegistry = new ComponentDefinitionRegistry(
            applicationProperties,
            List.of(
                ACCOUNTING_COMPONENT_HANDLER, CRM_COMPONENT_HANDLER, PLAIN_COMPONENT_HANDLER,
                TICKETING_COMPONENT_HANDLER),
            List::of, List.of());

        return new UnifiedApiDefinitionServiceImpl(componentDefinitionRegistry);
    }
}
