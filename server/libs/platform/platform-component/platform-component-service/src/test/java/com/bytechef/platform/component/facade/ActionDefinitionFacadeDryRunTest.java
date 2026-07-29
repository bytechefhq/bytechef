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

package com.bytechef.platform.component.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.domain.OutputResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ActionDefinitionFacadeDryRunTest {

    @Mock
    private ActionDefinitionService actionDefinitionService;

    @Mock
    private ConnectionService connectionService;

    private ActionDefinitionFacadeImpl actionDefinitionFacade;

    @BeforeEach
    void setUp() {
        actionDefinitionFacade = new ActionDefinitionFacadeImpl(connectionService, actionDefinitionService, null);
    }

    @Test
    void testReturnsDeclaredSampleOutput() {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.isOutputDefined()).thenReturn(true);
        when(actionDefinition.getOutputResponse())
            .thenReturn(new OutputResponse(null, Map.of("greeting", "hi")));
        when(actionDefinitionService.getActionDefinition("slack", 1, "sendMessage")).thenReturn(actionDefinition);

        assertThat(actionDefinitionFacade.executeDryRunPerform("slack", 1, "sendMessage"))
            .isEqualTo(Map.of("greeting", "hi"));
    }

    @Test
    void testReturnsEmptyMapWhenOutputNotDefined() {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.isOutputDefined()).thenReturn(false);
        when(actionDefinitionService.getActionDefinition("slack", 1, "sendMessage")).thenReturn(actionDefinition);

        assertThat(actionDefinitionFacade.executeDryRunPerform("slack", 1, "sendMessage")).isEqualTo(Map.of());
    }
}
