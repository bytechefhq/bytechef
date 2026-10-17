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

package com.bytechef.component.whatsapp.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class WhatsAppApprovalChannelTest {

    private static final String FORM_URL = "https://example.com/approval/form-path-segment";
    private static final String RESUME_ID = "signed.resume-token";

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ClusterElementContext context = mock(ClusterElementContext.class);
    private final Http.Executor executor = mock(Http.Executor.class);

    @BeforeEach
    void setUp() {
        Http.Response response = mock(Http.Response.class);

        when(context.http(any())).thenReturn(executor);
        when(executor.body(bodyArgumentCaptor.capture())).thenReturn(executor);
        when(executor.configuration(any())).thenReturn(executor);
        when(executor.execute()).thenReturn(response);
        when(response.getBody(any(TypeReference.class))).thenReturn(Map.of());
    }

    @Test
    void testInPlaceButtonsCarryTheResumeIdParameterRatherThanTheFormUrlTail() throws Exception {
        perform(inputParameters(Map.of(ApprovalChannelFunction.RESUME_ID, RESUME_ID)), "app-secret");

        assertThat(buttonIds()).containsExactly("a:" + RESUME_ID, "d:" + RESUME_ID);
    }

    @Test
    void testMissingResumeIdFallsBackToFormLinks() throws Exception {
        perform(inputParameters(Map.of()), "app-secret");

        Map<String, Object> content = bodyContent();

        assertThat(content).containsEntry("type", "text");
        assertThat(String.valueOf(content.get("text"))).contains(FORM_URL + "?approved=true");
    }

    @Test
    void testMissingAppSecretFallsBackToFormLinks() throws Exception {
        perform(inputParameters(Map.of(ApprovalChannelFunction.RESUME_ID, RESUME_ID)), null);

        assertThat(bodyContent()).containsEntry("type", "text");
    }

    private void perform(Parameters inputParameters, String appSecret) throws Exception {
        Map<String, Object> connectionParameters = new HashMap<>();

        connectionParameters.put("phoneNumberId", "1234");

        if (appSecret != null) {
            connectionParameters.put("appSecret", appSecret);
        }

        ApprovalChannelFunction approvalChannelFunction = WhatsAppApprovalChannel.CLUSTER_ELEMENT_DEFINITION
            .getElement();

        approvalChannelFunction.apply(
            inputParameters, MockParametersFactory.create(connectionParameters), FORM_URL, context);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyContent() {
        Body body = bodyArgumentCaptor.getValue();

        return (Map<String, Object>) body.getContent();
    }

    @SuppressWarnings("unchecked")
    private List<String> buttonIds() {
        Map<String, ?> interactive = (Map<String, ?>) bodyContent().get("interactive");
        Map<String, ?> action = (Map<String, ?>) interactive.get("action");
        List<Map<String, Map<String, String>>> buttons = (List<Map<String, Map<String, String>>>) action.get(
            "buttons");

        return buttons.stream()
            .map(button -> button.get("reply")
                .get("id"))
            .toList();
    }

    private static Parameters inputParameters(Map<String, Object> extraParameters) {
        Map<String, Object> parameters = new HashMap<>(extraParameters);

        parameters.put("to", "+15550001");
        parameters.put("inputs", List.of());

        return MockParametersFactory.create(parameters);
    }
}
