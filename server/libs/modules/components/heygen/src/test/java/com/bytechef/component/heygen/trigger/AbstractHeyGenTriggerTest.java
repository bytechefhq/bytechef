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

package com.bytechef.component.heygen.trigger;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.heygen.util.HeyGenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class AbstractHeyGenTriggerTest {

    protected WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    protected TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    protected MockedStatic<HeyGenUtils> heyGenUtilsMockedStatic;
    protected ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    protected ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor = forClass(TriggerContext.class);
    protected ArgumentCaptor<WebhookBody> webhookBodyArgumentCaptor = forClass(WebhookBody.class);

    @BeforeEach
    void beforeEach() {
        heyGenUtilsMockedStatic = mockStatic(HeyGenUtils.class);
    }

    @AfterEach
    void afterEach() {
        heyGenUtilsMockedStatic.close();
    }
}
