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

package com.bytechef.component.webhook.util;

import static com.bytechef.component.webhook.constant.WebhookConstants.CSRF_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WebhookUtilsTest {

    private final HttpHeaders httpHeaders = mock(HttpHeaders.class);
    private final HttpParameters httpParameters = mock(HttpParameters.class);
    private final TriggerContext triggerContext = mock(TriggerContext.class);
    private final WebhookBody webhookBody = mock(WebhookBody.class);

    @Test
    void testGetWebhookValidateReturnsOkWhenCsrfTokenMatchesHeader() {
        when(httpHeaders.firstValue("X-Csrf-Token")).thenReturn(Optional.of("token"));

        WebhookValidateResponse webhookValidateResponse = validate(MockParametersFactory.create(
            Map.of(CSRF_TOKEN, "token")));

        assertThat(webhookValidateResponse.status()).isEqualTo(200);
    }

    @Test
    void testGetWebhookValidateReturnsBadRequestWhenCsrfTokenDoesNotMatchHeader() {
        when(httpHeaders.firstValue("X-Csrf-Token")).thenReturn(Optional.of("other"));

        WebhookValidateResponse webhookValidateResponse = validate(MockParametersFactory.create(
            Map.of(CSRF_TOKEN, "token")));

        assertThat(webhookValidateResponse.status()).isEqualTo(400);
    }

    @Test
    void testGetWebhookValidateReturnsBadRequestWhenCsrfTokenIsNotSet() {
        when(httpHeaders.firstValue("X-Csrf-Token")).thenReturn(Optional.of("token"));

        WebhookValidateResponse webhookValidateResponse = validate(MockParametersFactory.create(Map.of()));

        assertThat(webhookValidateResponse.status()).isEqualTo(400);
    }

    @Test
    void testGetWebhookValidateReturnsBadRequestWhenCsrfTokenAndHeaderAreNotSet() {
        when(httpHeaders.firstValue("X-Csrf-Token")).thenReturn(Optional.empty());

        Map<String, Object> inputParameterMap = new HashMap<>();

        inputParameterMap.put(CSRF_TOKEN, null);

        WebhookValidateResponse webhookValidateResponse = validate(MockParametersFactory.create(inputParameterMap));

        assertThat(webhookValidateResponse.status()).isEqualTo(400);
    }

    private WebhookValidateResponse validate(Parameters inputParameters) {
        return WebhookUtils.getWebhookValidate(
            inputParameters, httpHeaders, httpParameters, webhookBody, WebhookMethod.POST, triggerContext);
    }
}
