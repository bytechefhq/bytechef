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

package com.bytechef.component.woocommerce.trigger;

import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.woocommerce.util.WoocommerceUtils;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
abstract class AbstractWoocommerceTriggerTest {

    protected final WebhookEnableOutput mockedWebhookEnableOutput = mock(WebhookEnableOutput.class);
    protected final Parameters mockedWebhookEnableOutputParameters = mock(Parameters.class);
    protected final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    protected final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    protected final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    protected final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    protected Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, 123));
    protected final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    protected final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    protected final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    protected final ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor =
        ArgumentCaptor.forClass(TriggerContext.class);
    protected MockedStatic<WoocommerceUtils> woocommerceUtilsMockedStatic;

    @BeforeEach
    void beforeEach() {
        woocommerceUtilsMockedStatic = mockStatic(WoocommerceUtils.class);
    }

    @AfterEach
    void afterEach() {
        woocommerceUtilsMockedStatic.close();
    }
}
