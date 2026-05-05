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

package com.bytechef.component.linear.trigger;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.linear.util.LinearUtils;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
abstract class AbstractLinearTriggerTest {

    protected WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected MockedStatic<LinearUtils> linearUtilsMockedStatic;
    protected ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    protected ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    protected ArgumentCaptor<WebhookBody> webhookBodyArgumentCaptor = forClass(WebhookBody.class);
    protected ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor = forClass(TriggerContext.class);

    @BeforeEach
    void beforeEach() {
        linearUtilsMockedStatic = mockStatic(LinearUtils.class);
    }

    @AfterEach
    void afterEach() {
        linearUtilsMockedStatic.close();
    }
}
