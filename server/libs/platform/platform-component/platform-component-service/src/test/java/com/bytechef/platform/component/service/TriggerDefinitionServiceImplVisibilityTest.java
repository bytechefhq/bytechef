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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class TriggerDefinitionServiceImplVisibilityTest {

    @Test
    void testExecuteTriggerRejectsDisabledComponent() {
        ComponentVisibilityProvider disableSlack = componentName -> !componentName.equals("slack");

        TriggerDefinitionServiceImpl service = new TriggerDefinitionServiceImpl(
            mock(ComponentDefinitionRegistry.class), mock(ContextFactory.class),
            mock(ApplicationEventPublisher.class), List.of(disableSlack));

        assertThatThrownBy(
            () -> service.executeTrigger(
                "slack", 1, "newMessage", 1L, "uuid", Map.of(), null, null, null, 1L, PlatformType.AUTOMATION,
                false))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("disabled");
    }
}
