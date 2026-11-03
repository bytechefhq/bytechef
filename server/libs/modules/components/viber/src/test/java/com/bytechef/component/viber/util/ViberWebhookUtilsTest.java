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

package com.bytechef.component.viber.util;

import static com.bytechef.component.viber.constant.ViberConstants.AUTH_TOKEN;
import static com.bytechef.component.viber.constant.ViberConstants.WELCOME_MESSAGE;
import static com.bytechef.component.viber.constant.ViberConstants.WELCOME_SENDER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import com.bytechef.component.viber.PlatformWebhookBody;
import com.bytechef.component.viber.trigger.ViberNewEventTrigger;
import com.bytechef.component.viber.trigger.ViberNewMessageTrigger;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockContextSetupExtension.class, ObjectMapperSetupExtension.class
})
class ViberWebhookUtilsTest {

    private static final String AUTH_TOKEN_VALUE = "test-viber-auth-token";
    private static final String DEPLOYED_URL = "https://bytechef.example.com/webhooks/abc";
    private static final String MESSAGE_BODY =
        "{\"event\":\"message\",\"message\":{\"type\":\"text\",\"text\":\"Hi\"}}";
    private static final String OTHER_DEPLOYED_URL = "https://bytechef.example.com/webhooks/other";
    private static final String TEST_URL = "https://bytechef.example.com/api/webhooks/test/environments/1/abc";

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters connectionParameters = MockParametersFactory.create(
        Map.of(AUTH_TOKEN, AUTH_TOKEN_VALUE));
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testValidateAcceptsValidSignature(TriggerContext mockedContext) {
        WebhookValidateResponse response = ViberWebhookUtils.validate(
            MockParametersFactory.create(Map.of()), connectionParameters,
            mockHeaders("X-Viber-Content-Signature", sign(MESSAGE_BODY)), mockedHttpParameters,
            mockBody(MESSAGE_BODY, Map.of("event", "message")), WebhookMethod.POST, mockedContext);

        assertEquals(200, response.status());
        assertNull(response.body());
    }

    @Test
    void testValidateRejectsWrongSignature(TriggerContext mockedContext) {
        WebhookValidateResponse response = ViberWebhookUtils.validate(
            MockParametersFactory.create(Map.of()), connectionParameters,
            mockHeaders("X-Viber-Content-Signature", sign(MESSAGE_BODY + " ")), mockedHttpParameters,
            mockBody(MESSAGE_BODY, Map.of("event", "message")), WebhookMethod.POST, mockedContext);

        assertEquals(401, response.status());
    }

    @Test
    void testValidateRejectsMissingSignatureHeader(TriggerContext mockedContext) {
        WebhookValidateResponse response = ViberWebhookUtils.validate(
            MockParametersFactory.create(Map.of()), connectionParameters,
            mockHeaders("Content-Type", "application/json"),
            mockedHttpParameters, mockBody(MESSAGE_BODY, Map.of("event", "message")), WebhookMethod.POST,
            mockedContext);

        assertEquals(401, response.status());
    }

    @Test
    void testValidateMatchesSignatureHeaderCaseInsensitively(TriggerContext mockedContext) {
        WebhookValidateResponse response = ViberWebhookUtils.validate(
            MockParametersFactory.create(Map.of()), connectionParameters,
            mockHeaders("x-viber-content-signature", sign(MESSAGE_BODY)), mockedHttpParameters,
            mockBody(MESSAGE_BODY, Map.of("event", "message")), WebhookMethod.POST, mockedContext);

        assertEquals(200, response.status());
    }

    @Test
    void testValidateAcceptsUppercaseSignature(TriggerContext mockedContext) {
        WebhookValidateResponse response = ViberWebhookUtils.validate(
            MockParametersFactory.create(Map.of()), connectionParameters,
            mockHeaders("X-Viber-Content-Signature", sign(MESSAGE_BODY).toUpperCase(Locale.ROOT)),
            mockedHttpParameters, mockBody(MESSAGE_BODY, Map.of("event", "message")), WebhookMethod.POST,
            mockedContext);

        assertEquals(200, response.status());
    }

    @Test
    void testValidateSignsNonAsciiBody(TriggerContext mockedContext) {
        String rawBody = "{\"event\":\"message\",\"message\":{\"type\":\"text\",\"text\":\"Здраво 👋\"}}";

        WebhookValidateResponse response = ViberWebhookUtils.validate(
            MockParametersFactory.create(Map.of()), connectionParameters,
            mockHeaders("X-Viber-Content-Signature", sign(rawBody)), mockedHttpParameters,
            mockBody(rawBody, Map.of("event", "message")), WebhookMethod.POST, mockedContext);

        assertEquals(200, response.status());
    }

    @Test
    void testValidateReturnsWelcomeBodyOnConversationStarted(TriggerContext mockedContext) {
        String rawBody = "{\"event\":\"conversation_started\",\"user\":{\"id\":\"user-1\"}}";

        WebhookValidateResponse response = ViberWebhookUtils.validate(
            MockParametersFactory.create(Map.of(WELCOME_MESSAGE, "Hi there", WELCOME_SENDER_NAME, "Acme")),
            connectionParameters, mockHeaders("X-Viber-Content-Signature", sign(rawBody)), mockedHttpParameters,
            mockBody(rawBody, Map.of("event", "conversation_started")), WebhookMethod.POST, mockedContext);

        assertEquals(200, response.status());
        assertEquals(
            Map.of("sender", Map.of("name", "Acme"), "type", "text", "text", "Hi there"), response.body());
    }

    @Test
    void testValidateReturnsPlainOkWithoutWelcomeMessage(TriggerContext mockedContext) {
        String rawBody = "{\"event\":\"conversation_started\",\"user\":{\"id\":\"user-1\"}}";

        WebhookValidateResponse response = ViberWebhookUtils.validate(
            MockParametersFactory.create(Map.of(WELCOME_SENDER_NAME, "Acme")), connectionParameters,
            mockHeaders("X-Viber-Content-Signature", sign(rawBody)), mockedHttpParameters,
            mockBody(rawBody, Map.of("event", "conversation_started")), WebhookMethod.POST, mockedContext);

        assertEquals(200, response.status());
        assertNull(response.body());
    }

    @Test
    void testValidateReadsPlatformParsedJsonBodyForNewMessage(TriggerContext mockedContext) {
        WebhookValidateResponse response = ViberWebhookUtils.validate(
            MockParametersFactory.create(Map.of()), connectionParameters,
            mockHeaders("X-Viber-Content-Signature", sign(MESSAGE_BODY)), mockedHttpParameters,
            PlatformWebhookBody.of(MESSAGE_BODY, ViberNewMessageTrigger.TRIGGER_DEFINITION), WebhookMethod.POST,
            mockedContext);

        assertEquals(200, response.status());
        assertNull(response.body());
    }

    @Test
    void testValidateReadsPlatformParsedJsonBodyForWelcomeMessage(TriggerContext mockedContext) {
        String rawBody = "{\"event\":\"conversation_started\",\"user\":{\"id\":\"user-1\"}}";

        WebhookValidateResponse response = ViberWebhookUtils.validate(
            MockParametersFactory.create(Map.of(WELCOME_MESSAGE, "Hi there", WELCOME_SENDER_NAME, "Acme")),
            connectionParameters, mockHeaders("X-Viber-Content-Signature", sign(rawBody)), mockedHttpParameters,
            PlatformWebhookBody.of(rawBody, ViberNewEventTrigger.TRIGGER_DEFINITION), WebhookMethod.POST,
            mockedContext);

        assertEquals(200, response.status());
        assertEquals(
            Map.of("sender", Map.of("name", "Acme"), "type", "text", "text", "Hi there"), response.body());
    }

    @Test
    void testEnableWebhookSetsWebhookWhenNoneExists(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(
            mockedResponse, mockedExecutor, mockedHttp, Map.of("status", 0, "name", "Bot"), Map.of("status", 0));

        WebhookEnableOutput webhookEnableOutput = ViberWebhookUtils.enableWebhook(
            DEPLOYED_URL, List.of("conversation_started"), mockedContext);

        assertEquals(Map.of("webhookUrl", DEPLOYED_URL), webhookEnableOutput.parameters());
        assertEquals(List.of("/get_account_info", "/set_webhook"), stringArgumentCaptor.getAllValues());
        assertEquals(
            Body.of(
                Map.of(
                    "url", DEPLOYED_URL, "event_types", List.of("conversation_started"), "send_name", true,
                    "send_photo", true),
                BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testEnableWebhookAllowsSameUrl(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(
            mockedResponse, mockedExecutor, mockedHttp, Map.of("status", 0, "webhook", DEPLOYED_URL),
            Map.of("status", 0));

        ViberWebhookUtils.enableWebhook(DEPLOYED_URL, List.of("message"), mockedContext);

        assertEquals(List.of("/get_account_info", "/set_webhook"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testEnableWebhookRejectsOtherWorkflowUrl(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(
            mockedResponse, mockedExecutor, mockedHttp, Map.of("status", 0, "webhook", OTHER_DEPLOYED_URL),
            Map.of("status", 0));

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> ViberWebhookUtils.enableWebhook(DEPLOYED_URL, List.of("message"), mockedContext));

        assertTrue(providerException.getMessage()
            .contains(OTHER_DEPLOYED_URL));
        assertEquals(List.of("/get_account_info"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testEnableWebhookReplacesLeftoverTestUrlOnDeploy(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(
            mockedResponse, mockedExecutor, mockedHttp, Map.of("status", 0, "webhook", TEST_URL),
            Map.of("status", 0));

        ViberWebhookUtils.enableWebhook(DEPLOYED_URL, List.of("message"), mockedContext);

        assertEquals(List.of("/get_account_info", "/set_webhook"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testEnableWebhookRejectsDeployedUrlFromTestSession(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(
            mockedResponse, mockedExecutor, mockedHttp, Map.of("status", 0, "webhook", DEPLOYED_URL),
            Map.of("status", 0));

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> ViberWebhookUtils.enableWebhook(TEST_URL, List.of("message"), mockedContext));

        assertTrue(providerException.getMessage()
            .contains(DEPLOYED_URL));
        assertEquals(List.of("/get_account_info"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testEnableWebhookFailsWhenViberRejectsUrl(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(
            mockedResponse, mockedExecutor, mockedHttp, Map.of("status", 0),
            Map.of("status", 1, "status_message", "invalidUrl"));

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> ViberWebhookUtils.enableWebhook(DEPLOYED_URL, List.of("message"), mockedContext));

        assertTrue(providerException.getMessage()
            .contains("invalidUrl (1)"));
    }

    @Test
    void testDisableWebhookClearsOwnUrl(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(
            mockedResponse, mockedExecutor, mockedHttp, Map.of("status", 0, "webhook", DEPLOYED_URL),
            Map.of("status", 0));

        ViberWebhookUtils.disableWebhook(DEPLOYED_URL, mockedContext);

        assertEquals(List.of("/get_account_info", "/set_webhook"), stringArgumentCaptor.getAllValues());
        assertEquals(Body.of(Map.of("url", ""), BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }

    @Test
    void testDisableWebhookLeavesOtherUrlAlone(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(
            mockedResponse, mockedExecutor, mockedHttp, Map.of("status", 0, "webhook", OTHER_DEPLOYED_URL),
            Map.of("status", 0));

        ViberWebhookUtils.disableWebhook(DEPLOYED_URL, mockedContext);

        assertEquals(List.of("/get_account_info"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testIsTestWebhookUrl() {
        assertTrue(ViberWebhookUtils.isTestWebhookUrl(TEST_URL));
        assertEquals(false, ViberWebhookUtils.isTestWebhookUrl(DEPLOYED_URL));
    }

    @Test
    void testValidateOnEnableAlwaysReturnsOk(TriggerContext mockedContext) {
        WebhookValidateResponse response = ViberWebhookUtils.validateOnEnable(
            MockParametersFactory.create(Map.of()), mockHeaders("Content-Type", "application/json"),
            mockedHttpParameters, mockBody("{}", Map.of()), WebhookMethod.POST, mockedContext);

        assertEquals(200, response.status());
    }

    private static WebhookBody mockBody(String rawContent, Map<String, Object> content) {
        WebhookBody webhookBody = mock(WebhookBody.class);

        when(webhookBody.getRawContent()).thenReturn(rawContent);
        when(webhookBody.getContent(any(TypeReference.class))).thenReturn(new HashMap<>(content));

        return webhookBody;
    }

    private static HttpHeaders mockHeaders(String name, String value) {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);

        when(httpHeaders.toMap()).thenReturn(Map.of(name, List.of(value)));

        return httpHeaders;
    }

    private static String sign(String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(AUTH_TOKEN_VALUE.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            return HexFormat.of()
                .formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void stubHttp(
        Response mockedResponse, Executor mockedExecutor, Http mockedHttp, Map<String, Object> accountInfo,
        Map<String, Object> setWebhookResponse) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(accountInfo, setWebhookResponse);
    }
}
