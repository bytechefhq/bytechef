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

package com.bytechef.component.attio.trigger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.attio.util.AttioUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
public class AbstractAttioTriggerTest {

    protected Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    protected WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    protected HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    protected HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    protected WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    protected MockedStatic<AttioUtils> attioUtilsMockedStatic;
    protected ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    protected ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor = ArgumentCaptor.forClass(
        TriggerContext.class);
    protected ArgumentCaptor<WebhookBody> webhookBodyArgumentCaptor = ArgumentCaptor.forClass(WebhookBody.class);
    protected String workflowExecutionId = "testWorkflowExecutionId";

    @BeforeEach
    void beforeEach() {
        attioUtilsMockedStatic = mockStatic(AttioUtils.class);
    }

    @AfterEach
    void afterEach() {
        attioUtilsMockedStatic.close();
    }
}
