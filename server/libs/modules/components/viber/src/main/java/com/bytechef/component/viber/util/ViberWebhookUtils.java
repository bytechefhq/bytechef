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
import static com.bytechef.component.viber.constant.ViberConstants.CONVERSATION_STARTED;
import static com.bytechef.component.viber.constant.ViberConstants.WEBHOOK;
import static com.bytechef.component.viber.constant.ViberConstants.WELCOME_MESSAGE;
import static com.bytechef.component.viber.constant.ViberConstants.WELCOME_SENDER_NAME;

import com.bytechef.component.definition.Context.Http;
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
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class ViberWebhookUtils {

    private static final Logger log = LoggerFactory.getLogger(ViberWebhookUtils.class);

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Viber-Content-Signature";
    private static final String TEST_WEBHOOK_URL_MARKER = "/test/environments/";

    private ViberWebhookUtils() {
    }

    public static WebhookEnableOutput
        enableWebhook(String webhookUrl, List<String> eventTypes, TriggerContext context) {
        String currentWebhookUrl = getCurrentWebhookUrl(context);

        if (currentWebhookUrl != null && !currentWebhookUrl.equals(webhookUrl) &&
            (isTestWebhookUrl(webhookUrl) || !isTestWebhookUrl(currentWebhookUrl))) {

            throw new ProviderException(
                ("This Viber bot already sends its messages to another workflow (%s). Disable that workflow first, " +
                    "or clear the bot's webhook.").formatted(currentWebhookUrl));
        }

        setWebhook(
            Map.of("url", webhookUrl, "event_types", eventTypes, "send_name", true, "send_photo", true), context);

        return new WebhookEnableOutput(Map.of("webhookUrl", webhookUrl), null);
    }

    public static void disableWebhook(@Nullable String webhookUrl, TriggerContext context) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            if (log.isWarnEnabled()) {
                log.warn("Skipping the Viber webhook removal because the enabled webhook URL is unknown");
            }

            return;
        }

        if (webhookUrl.equals(getCurrentWebhookUrl(context))) {
            setWebhook(Map.of("url", ""), context);
        }
    }

    public static boolean isTestWebhookUrl(String webhookUrl) {
        return webhookUrl.contains(TEST_WEBHOOK_URL_MARKER);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public static WebhookValidateResponse validate(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, TriggerContext context) {

        String signature = findSignature(headers);

        if (signature == null) {
            return WebhookValidateResponse.unauthorized();
        }

        String rawContent = body.getRawContent();

        String expectedSignature = calculateSignature(
            Objects.requireNonNullElse(rawContent, ""), connectionParameters.getRequiredString(AUTH_TOKEN));

        if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {

            return WebhookValidateResponse.unauthorized();
        }

        Map<String, Object> event = body.getContent(new TypeReference<>() {});
        String welcomeMessage = inputParameters.getString(WELCOME_MESSAGE);

        if (event != null && CONVERSATION_STARTED.equals(event.get("event")) && welcomeMessage != null &&
            !welcomeMessage.isBlank()) {

            String senderName = ViberUtils.resolveSenderName(inputParameters.getString(WELCOME_SENDER_NAME), context);

            return WebhookValidateResponse.ok(
                Map.of("sender", Map.of("name", senderName), "type", "text", "text", welcomeMessage));
        }

        return WebhookValidateResponse.ok();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public static WebhookValidateResponse validateOnEnable(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        return WebhookValidateResponse.ok();
    }

    private static String calculateSignature(String content, String authToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);

            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));

            byte[] hashBytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of()
                .formatHex(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new ProviderException(exception);
        }
    }

    private static @Nullable String findSignature(HttpHeaders headers) {
        Map<String, List<String>> headerMap = headers.toMap();

        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            List<String> values = entry.getValue();

            if (SIGNATURE_HEADER.equalsIgnoreCase(entry.getKey()) && values != null && !values.isEmpty()) {
                String signature = values.getFirst();

                return signature == null ? null : signature.toLowerCase(Locale.ROOT);
            }
        }

        return null;
    }

    private static @Nullable String getCurrentWebhookUrl(TriggerContext context) {
        Map<String, Object> accountInfo = ViberUtils.getAccountInfo(context);

        Object webhook = accountInfo.get(WEBHOOK);

        if (webhook instanceof String webhookUrl && !webhookUrl.isBlank()) {
            return webhookUrl;
        }

        return null;
    }

    private static void setWebhook(Map<String, Object> body, TriggerContext context) {
        Map<String, Object> response = context
            .http(http -> http.post("/set_webhook"))
            .body(Http.Body.of(body, Http.BodyContentType.JSON))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        ViberUtils.checkStatus(response);
    }
}
