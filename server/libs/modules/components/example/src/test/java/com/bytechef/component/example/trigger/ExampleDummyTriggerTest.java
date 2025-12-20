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

package com.bytechef.component.example.trigger;

import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExampleDummyTriggerTest {

    @Test
    void testWebhookEnable() {
        Parameters parameters = mock(Parameters.class);
        String webhookUrl = "testWebhookUrl";
        String workflowExecutionId = "testWorkflowExecutionId";

        Assertions.assertNull(
            ExampleDummyTrigger.webhookEnable(parameters, parameters, webhookUrl, workflowExecutionId,
                mock(TriggerContext.class)));
    }

    @Test
    void testWebhookRequest() {
        Parameters parameters = mock(Parameters.class);

        Assertions.assertNull(
            ExampleDummyTrigger.webhookRequest(parameters, parameters, mock(TriggerDefinition.HttpHeaders.class),
                mock(TriggerDefinition.HttpParameters.class), mock(TriggerDefinition.WebhookBody.class),
                mock(TriggerDefinition.WebhookMethod.class), mock(Parameters.class), mock(TriggerContext.class)));
    }

}
