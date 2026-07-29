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

package com.bytechef.platform.workflow.worker.trigger.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.worker.exception.TriggerExecutionException;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class AbstractTriggerHandlerDryRunTest {

    @Mock
    private TriggerDefinitionFacade triggerDefinitionFacade;

    private AbstractTriggerHandler handler;

    @BeforeEach
    void beforeEach() {
        handler = new AbstractTriggerHandler("slack", 1, "newMessage", triggerDefinitionFacade) {};
    }

    @Test
    void testDryRunReturnsDeclaredOutputWithoutExecutingTrigger() throws TriggerExecutionException {
        when(triggerDefinitionFacade.executeDryRunTrigger("slack", 1, "newMessage")).thenReturn(Map.of("ok", true));

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .metadata(Map.of(MetadataConstants.DRY_RUN, true))
            .build();

        TriggerOutput triggerOutput = handler.handle(triggerExecution);

        assertThat(triggerOutput.value()).isEqualTo(Map.of("ok", true));
        assertThat(triggerOutput.state()).isNull();
        assertThat(triggerOutput.batch()).isFalse();

        verify(triggerDefinitionFacade, never()).executeTrigger(
            any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    }
}
