/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.xero.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.xero.constant.XeroConstants.CODE;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACTS;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_ID;
import static com.bytechef.component.xero.constant.XeroConstants.CREATE;
import static com.bytechef.component.xero.constant.XeroConstants.CURRENCY_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.DATE;
import static com.bytechef.component.xero.constant.XeroConstants.DUE_DATE;
import static com.bytechef.component.xero.constant.XeroConstants.INVOICE;
import static com.bytechef.component.xero.constant.XeroConstants.INVOICES;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_AMOUNT_TYPES;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEMS;
import static com.bytechef.component.xero.constant.XeroConstants.MESSAGE;
import static com.bytechef.component.xero.constant.XeroConstants.NAME;
import static com.bytechef.component.xero.constant.XeroConstants.REFERENCE;
import static com.bytechef.component.xero.constant.XeroConstants.TYPE;
import static com.bytechef.component.xero.constant.XeroConstants.WEBHOOK_KEY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Monika Domiter
 */
public class XeroUtils {

    protected static final ContextFunction<Http, Http.Executor> GET_ACCOUNTS_CONTEXT_FUNCTION =
        http -> http.get("/Accounts");

    protected static final ContextFunction<Http, Http.Executor> GET_BRANDING_THEME_CONTEXT_FUNCTION =
        http -> http.get("/BrandingTheme");

    protected static final ContextFunction<Http, Http.Executor> GET_CONTACTS_CONTEXT_FUNCTION =
        http -> http.get("/" + CONTACTS);

    protected static final ContextFunction<Http, Http.Executor> GET_CURRENCIES_CONTEXT_FUNCTION =
        http -> http.get("/Currencies");

    private XeroUtils() {
    }

    public static Object createInvoice(
        Parameters inputParameters, ActionContext actionContext, ContextFunction<Http, Http.Executor> contextFunction,
        String accpay) {

        Map<String, Object> body = actionContext.http(contextFunction)
            .body(
                Http.Body.of(
                    TYPE, accpay,
                    CONTACT, Map.of(CONTACT_ID, inputParameters.getRequiredString(CONTACT_ID)),
                    DATE, inputParameters.getLocalDate(DATE, LocalDate.now()),
                    DUE_DATE, inputParameters.getLocalDate(DUE_DATE, LocalDate.now()),
                    REFERENCE, inputParameters.getString(REFERENCE),
                    CURRENCY_CODE, inputParameters.getString(CURRENCY_CODE),
                    LINE_AMOUNT_TYPES, inputParameters.getString(LINE_AMOUNT_TYPES),
                    LINE_ITEMS, inputParameters.getList(LINE_ITEMS)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(INVOICES) instanceof List<?> list) {
            return list.getFirst();
        } else {
            return body.get(MESSAGE);
        }
    }

    public static List<Option<String>> getAccountCodeOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(GET_ACCOUNTS_CONTEXT_FUNCTION)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("Accounts") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(CODE)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getBrandingThemeIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(GET_BRANDING_THEME_CONTEXT_FUNCTION)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("BrandingThemes") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get("BrandingThemeID")));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getContactIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(GET_CONTACTS_CONTEXT_FUNCTION)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get(CONTACTS) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get("ContactID")));
                }
            }
        }
        return options;

    }

    public static List<Option<String>> getCurrencyCodeOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(GET_CURRENCIES_CONTEXT_FUNCTION)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("Currencies") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("Description"), (String) map.get(CODE)));
                }
            }
        }

        return options;
    }

    public static Object getCreatedObject(
        WebhookBody body, TriggerContext context, String category, String invoiceType) {

        if (body.getContent() instanceof Map<?, ?> map && map.get("events") instanceof List<?> list) {
            Object object = list.getFirst();

            if (object instanceof Map<?, ?> eventMap) {
                String eventCategory = (String) eventMap.get("eventCategory");
                String eventType = (String) eventMap.get("eventType");

                if (eventCategory.equals(category) && eventType.equals(CREATE)) {
                    String resourceId = (String) eventMap.get("resourceId");
                    String urlPart = category.equals(INVOICE) ? INVOICES : CONTACTS;

                    Map<String, Object> objectsBody = context
                        .http(http -> http.get("/" + urlPart + "/" + resourceId))
                        .configuration(Http.responseType(Http.ResponseType.JSON))
                        .execute()
                        .getBody(new TypeReference<>() {});

                    if (objectsBody.get(urlPart) instanceof List<?> objects) {

                        if (category.equals(INVOICE)) {
                            Object o = objects.getFirst();

                            if (o instanceof Map<?, ?> invoiceMap && invoiceMap.get(TYPE)
                                .equals(invoiceType)) {
                                return o;
                            }
                        } else {
                            return objects.getFirst();
                        }
                    }
                }
            }
        }

        return null;
    }

    public static WebhookValidateResponse webhookValidate(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        Map<String, List<String>> headersMap = headers.toMap();

        String xeroSignature = headersMap.get("x-xero-signature")
            .getFirst();

        String content = body.getRawContent();

        String webhookKey = inputParameters.getRequiredString(WEBHOOK_KEY);

        String encodedData = calculateHmac(content, webhookKey);

        int status = encodedData.equals(xeroSignature) ? 200 : 401;

        return new WebhookValidateResponse(status);
    }

    private static String calculateHmac(String content, String webhookKey) {
        String algorithm = "HmacSHA256";

        try {
            Mac sha256HMAC = Mac.getInstance(algorithm);
            SecretKeySpec secretKey = new SecretKeySpec(webhookKey.getBytes(StandardCharsets.UTF_8), algorithm);
            sha256HMAC.init(secretKey);
            byte[] hashBytes = sha256HMAC.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder()
                .encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new ProviderException(e);
        }
    }
}
