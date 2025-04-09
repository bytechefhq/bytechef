/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.bolna.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class BolnaCallCompletionReportTriggerTest {

    private final TriggerDefinition.WebhookEnableOutput mockedWebhookEnableOutput =
        mock(TriggerDefinition.WebhookEnableOutput.class);
    private final TriggerDefinition.WebhookBody mockedWebhookBody = mock(TriggerDefinition.WebhookBody.class);
    private final TriggerDefinition.HttpHeaders mockedHttpHeaders = mock(TriggerDefinition.HttpHeaders.class);
    private final TriggerDefinition.HttpParameters mockedHttpParameters = mock(TriggerDefinition.HttpParameters.class);
    private final TriggerDefinition.WebhookMethod mockedWebhookMethod = mock(TriggerDefinition.WebhookMethod.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent())
            .thenReturn(mockedObject);

        Object result = BolnaCallCompletionReportTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(mockedObject, result);
        verify(mockedWebhookBody).getContent();
    }
}
