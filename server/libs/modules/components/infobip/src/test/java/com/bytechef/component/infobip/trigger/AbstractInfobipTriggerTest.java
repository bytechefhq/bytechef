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

package com.bytechef.component.infobip.trigger;

import static com.bytechef.component.infobip.constant.InfobipConstants.CONFIGURATION_KEY;
import static com.bytechef.component.infobip.constant.InfobipConstants.NUMBER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.infobip.util.InfobipUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
abstract class AbstractInfobipTriggerTest {

    protected MockedStatic<InfobipUtils> infobipUtilsMockedStatic;
    protected Parameters mockedParameters = MockParametersFactory.create(
        Map.of(NUMBER, "123", CONFIGURATION_KEY, "abc"));
    protected TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    protected WebhookEnableOutput mockedWebhookEnableOutput = mock(WebhookEnableOutput.class);
    protected ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    protected ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor =
        ArgumentCaptor.forClass(TriggerContext.class);

    @BeforeEach
    void beforeEach() {
        infobipUtilsMockedStatic = mockStatic(InfobipUtils.class);
    }

    @AfterEach
    void afterEach() {
        infobipUtilsMockedStatic.close();
    }

}
