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

import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
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
class TriggerDefinitionFacadeDryRunTest {

    @Mock
    private ConnectionService connectionService;

    @Mock
    private TriggerDefinitionService triggerDefinitionService;

    private TriggerDefinitionFacadeImpl triggerDefinitionFacade;

    @BeforeEach
    void setUp() {
        triggerDefinitionFacade = new TriggerDefinitionFacadeImpl(connectionService, triggerDefinitionService);
    }

    @Test
    void testReturnsDeclaredSampleOutput() {
        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.isOutputDefined()).thenReturn(true);
        when(triggerDefinition.getOutputResponse())
            .thenReturn(new OutputResponse(null, Map.of("greeting", "hi")));
        when(triggerDefinitionService.getTriggerDefinition("slack", 1, "newMessage"))
            .thenReturn(triggerDefinition);

        assertThat(triggerDefinitionFacade.executeDryRunTrigger("slack", 1, "newMessage"))
            .isEqualTo(Map.of("greeting", "hi"));
    }

    @Test
    void testReturnsEmptyMapWhenOutputNotDefined() {
        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.isOutputDefined()).thenReturn(false);
        when(triggerDefinitionService.getTriggerDefinition("slack", 1, "newMessage"))
            .thenReturn(triggerDefinition);

        assertThat(triggerDefinitionFacade.executeDryRunTrigger("slack", 1, "newMessage")).isEqualTo(Map.of());
    }
}
