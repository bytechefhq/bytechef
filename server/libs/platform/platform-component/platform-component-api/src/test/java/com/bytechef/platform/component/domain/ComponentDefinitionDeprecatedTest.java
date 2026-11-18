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

package com.bytechef.platform.component.domain;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ComponentDefinitionDeprecatedTest {

    @Test
    void testAComponentWhoseEveryActionIsDeprecatedIsDeprecated() {
        ComponentDefinition componentDefinition = new ComponentDefinition(
            component("legacy").actions(
                action("first").deprecated(true),
                action("second").deprecated(true)));

        assertThat(componentDefinition.isDeprecated()).isTrue();
        assertThat(componentDefinition.getActions())
            .allSatisfy(actionDefinition -> assertThat(actionDefinition.isDeprecated()).isTrue());
    }

    @Test
    void testAComponentWithOneCurrentActionIsNotDeprecated() {
        ComponentDefinition componentDefinition = new ComponentDefinition(
            component("mixed").actions(
                action("old").deprecated(true),
                action("current")));

        assertThat(componentDefinition.isDeprecated()).isFalse();
    }

    @Test
    void testAComponentWithATriggerIsNotDeprecated() {
        ComponentDefinition componentDefinition = new ComponentDefinition(
            component("triggered")
                .actions(action("old").deprecated(true))
                .triggers(trigger("event").type(TriggerType.POLLING)));

        assertThat(componentDefinition.isDeprecated()).isFalse();
    }

    @Test
    void testAComponentWithoutActionsIsNotDeprecated() {
        assertThat(new ComponentDefinition(component("empty")).isDeprecated()).isFalse();
        assertThat(new ComponentDefinition("named").isDeprecated()).isFalse();
    }
}
