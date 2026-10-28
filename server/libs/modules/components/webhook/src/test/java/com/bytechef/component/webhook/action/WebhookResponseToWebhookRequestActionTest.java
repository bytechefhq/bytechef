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

package com.bytechef.component.webhook.action;

import static com.bytechef.component.webhook.constant.WebhookConstants.BODY;
import static com.bytechef.component.webhook.constant.WebhookConstants.HEADERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.STATUS_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WebhookResponseToWebhookRequestActionTest {

    private final ActionContext actionContext = mock(ActionContext.class);
    private final Parameters connectionParameters = MockParametersFactory.create(Map.of());

    @Test
    void testOutputJsonBody() {
        OutputResponse outputResponse = output(
            Map.of("responseType", "JSON", BODY, Map.of("key", "value"), STATUS_CODE, 201));

        assertThat(outputResponse.getSampleOutput()).isEqualTo(
            Map.of(BODY, Map.of("key", "value"), HEADERS, Map.of(), STATUS_CODE, 201));
    }

    @Test
    void testOutputJsonBodyWhenResponseTypeIsNotSet() {
        OutputResponse outputResponse = output(Map.of(BODY, Map.of("key", "value")));

        assertThat(outputResponse.getSampleOutput()).isEqualTo(
            Map.of(BODY, Map.of("key", "value"), HEADERS, Map.of(), STATUS_CODE, 200));
    }

    @Test
    void testOutputRawBody() {
        OutputResponse outputResponse = output(Map.of("responseType", "RAW", BODY, "text"));

        assertThat(outputResponse.getSampleOutput()).isEqualTo(
            Map.of(BODY, "text", HEADERS, Map.of(), STATUS_CODE, 200));
    }

    @Test
    void testOutputRedirectUrl() {
        OutputResponse outputResponse = output(Map.of("responseType", "REDIRECT", BODY, "https://example.com"));

        assertThat(outputResponse.getSampleOutput()).isEqualTo(
            Map.of(BODY, "https://example.com", HEADERS, Map.of(), STATUS_CODE, 200));
    }

    @Test
    void testOutputNoDataHasNoBody() {
        OutputResponse outputResponse = output(Map.of("responseType", "NO_DATA", BODY, true));

        assertThat(outputResponse.getSampleOutput()).isEqualTo(Map.of(HEADERS, Map.of(), STATUS_CODE, 200));
    }

    @Test
    void testOutputNonMapJsonBody() {
        OutputResponse outputResponse = output(Map.of("responseType", "JSON", BODY, true));

        assertThat(outputResponse.getSampleOutput()).isEqualTo(
            Map.of(BODY, true, HEADERS, Map.of(), STATUS_CODE, 200));
    }

    private OutputResponse output(Map<String, Object> parameterMap) {
        return WebhookResponseToWebhookRequestAction.output(
            MockParametersFactory.create(new HashMap<>(parameterMap)), connectionParameters, actionContext);
    }
}
