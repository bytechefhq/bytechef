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

package com.bytechef.component.gaurus;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.google.auto.service.AutoService;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

/**
 * This class will not be overwritten on the repeated calls of the generator.
 *
 * @author Igor Beslic
 */
@AutoService(OpenApiComponentHandler.class)
public class GaurusComponentHandler extends AbstractGaurusComponentHandler {

    private static final String ALLOW_SELF_SIGNED_CERT = "allowSelfSignedCert";
    private static final String EMPTY_BODY = "";
    private static final String TYPE = "type";

    public ModifiableActionDefinition modifyAction(ModifiableActionDefinition modifiableActionDefinition) {
        Optional<Map<String, Object>> metadata = modifiableActionDefinition.getMetadata();
        Map<String, Object> metadataMap = metadata.orElseGet(Collections::emptyMap);

        Optional<OutputDefinition> outputDefinitionOptional = modifiableActionDefinition.getOutputDefinition();
        Http.ResponseType responseType;

        if (outputDefinitionOptional.isPresent()) {
            OutputDefinition outputDefinition = outputDefinitionOptional.get();
            BaseValueProperty<?> outputSchema = outputDefinition.getOutputSchema();

            responseType = outputSchema == null ? (Http.ResponseType) metadataMap.get("responseType")
                : (Http.ResponseType) outputSchema.getMetadata()
                    .get("responseType");
        } else {
            responseType = null;
        }

        String path = (String) metadataMap.get("path");

        modifiableActionDefinition.perform((PerformFunction) (inputParameters, connectionParameters, context) -> {
            Optional<List<? extends Property>> propertiesOptional = modifiableActionDefinition.getProperties();

            String normalizedPath = path;
            if (propertiesOptional.isPresent()) {
                List<? extends Property> properties = propertiesOptional.get();

                for (Property property : properties) {
                    Map<String, Object> propertyMetadata = property.getMetadata();

                    if (propertyMetadata.get(TYPE) == PropertyType.PATH) {
                        normalizedPath = normalizedPath.replace(
                            "{" + property.getName() + "}",
                            URLEncoder.encode(
                                inputParameters.getRequiredString(property.getName()), StandardCharsets.UTF_8));
                    }
                }
            }

            String clientId = connectionParameters.getRequiredString(CLIENT_ID);
            String clientSecret = connectionParameters.getRequiredString(CLIENT_SECRET);
            String requestId = asUuid(context.getTraceId());
            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()
                .truncatedTo(ChronoUnit.MILLIS));

            String body = EMPTY_BODY;
            String signature = getSignature(
                clientId, clientSecret, requestId, timestamp, getRelativeUrl(normalizedPath), body, context);

            String finalNormalizedPath = normalizedPath;
            context.log(log -> log.debug(
                "Executing endpoint: {} with clientId: {}, requestId: {}, timestamp: {}, uri: {}, body: {}, signature {}",
                finalNormalizedPath, clientId, requestId, timestamp, path, body, signature));

            Response response = context
                .http(
                    http -> http.exchange(finalNormalizedPath,
                        Http.RequestMethod.valueOf((String) metadataMap.get("method"))))
                .configuration(
                    Http.allowUnauthorizedCerts(connectionParameters.getBoolean(ALLOW_SELF_SIGNED_CERT, false))
                        .responseType(responseType))
                .headers(getHeaders(inputParameters, propertiesOptional, clientId, requestId, timestamp, signature))
                .queryParameters(getValuesMap(inputParameters, propertiesOptional, PropertyType.QUERY))
                .body(
                    getBody(
                        (BodyContentType) metadataMap.get("bodyContentType"),
                        (String) metadataMap.get("mimeType"), inputParameters, propertiesOptional))
                .execute();

            return response.getBody();
        });

        return modifiableActionDefinition;
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .authorizations(
                authorization(AuthorizationType.CUSTOM)
                    .title("Gaurus HmacSHA256 Authorization")
                    .properties(
                        string(CLIENT_ID)
                            .label("Client ID")
                            .description("Client Id generated at GAURUS")
                            .required(true),
                        string(CLIENT_SECRET)
                            .label("Client Secret")
                            .description("The secret key for digital signing")
                            .required(true),
                        bool(ALLOW_SELF_SIGNED_CERT)
                            .label("Allow Self-Signed Certificates")
                            .description("Allow secure connections to servers with self-signed certificates")
                            .defaultValue(false)))
            .version(1);
    }

    private static String getSignature(
        String clientId, String clientSecret, String requestId, String timestamp, String uri, String body,
        ActionContext context) {

        String forSigning = String.format("%s;%s;%s;%s;%s", clientId, requestId, timestamp, uri, body);

        context.log(log -> log.debug("Signing {} with {}", forSigning, clientSecret));

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            return context
                .encoder(encoder -> encoder.base64Encode(mac.doFinal(forSigning.getBytes(StandardCharsets.UTF_8))));
        } catch (InvalidKeyException | NoSuchAlgorithmException exception) {
            throw new RuntimeException("Failed to compute HmacSHA256 signature", exception);
        }
    }

    private static String asUuid(String rawUuid) {
        if (rawUuid.contains("-")) {
            return rawUuid;
        }

        return rawUuid.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
    }

    private static String getRelativeUrl(String uriString) {
        try {
            URI uri = new URI(uriString);

            return uri.getPath();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to compute HmacSHA256 signature", exception);
        }
    }

    private static @NonNull Map<String, List<String>> getHeaders(
        Parameters inputParameters, Optional<List<? extends Property>> propertiesOptional, String clientId,
        String requestId, String timestamp, String signature) {
        Map<String, List<String>> headers = getValuesMap(inputParameters, propertiesOptional, PropertyType.HEADER);

        headers.put(CLIENT_ID, List.of(clientId));
        headers.put("requestId", List.of(requestId));
        headers.put("timestamp", List.of(timestamp));
        headers.put("signature", List.of(signature));

        return headers;
    }

    private static Http.Body getBody(
        BodyContentType bodyContentType, String mimeType, Parameters inputParameters,
        Optional<List<? extends Property>> propertiesOptional) {

        if (bodyContentType == null) {
            return null;
        }

        if (propertiesOptional.isPresent()) {
            List<? extends Property> properties = propertiesOptional.get();

            List<? extends Property> bodyProperties = properties.stream()
                .filter(property -> Objects.equals(property.getMetadata()
                    .get(TYPE), PropertyType.BODY))
                .toList();

            if (bodyProperties.size() == 1) {
                Property bodyProperty = bodyProperties.getFirst();

                String name = bodyProperty.getName();

                return switch (bodyContentType) {
                    case BINARY -> Http.Body.of(inputParameters.get(name, FileEntry.class), mimeType);
                    case FORM_DATA -> Http.Body.of(
                        inputParameters.getMap(name, List.of(FileEntry.class), Map.of()), bodyContentType);
                    case FORM_URL_ENCODED -> Http.Body.of(
                        inputParameters.getMap(name, Map.of()), bodyContentType);
                    case JSON, XML -> {
                        Property.Type type = bodyProperty.getType();

                        if (type == Property.Type.ARRAY) {
                            List<Object> list = inputParameters.getList(name, Object.class, List.of());

                            if ("__items".equals(name)) {
                                yield Http.Body.of(list, bodyContentType);
                            }

                            yield Http.Body.of(Map.of(name, list), bodyContentType);
                        } else if (type == Property.Type.DYNAMIC_PROPERTIES) {
                            yield Http.Body.of(inputParameters.getMap(name, Map.of()), bodyContentType);
                        } else {
                            yield Http.Body.of(Map.of(name, inputParameters.get(name)), bodyContentType);
                        }
                    }
                    case RAW -> Http.Body.of(inputParameters.getString(name), mimeType);
                };
            } else if (bodyProperties.size() > 1) {
                Map<String, Object> body = new HashMap<>();

                for (Property property : bodyProperties) {
                    Property.Type type = property.getType();

                    if (type.equals(Property.Type.DYNAMIC_PROPERTIES)) {
                        String name = property.getName();

                        Map<String, ?> map = inputParameters.getMap(name, Map.of());

                        body.put(name, map.get(name));
                    } else {
                        Object value = inputParameters.get(property.getName());

                        if (value != null) {
                            body.put(property.getName(), value);
                        }
                    }
                }

                return Http.Body.of(body, bodyContentType);
            }
        }

        return null;
    }

    private static Map<String, List<String>> getValuesMap(
        Parameters inputParameters, Optional<List<? extends Property>> propertiesOptional, PropertyType propertyType) {

        Map<String, List<String>> valuesMap = new HashMap<>();

        if (propertiesOptional.isPresent()) {
            List<? extends Property> properties = propertiesOptional.get();
            for (Property property : properties) {
                if (Objects.equals(property.getMetadata()
                    .get(TYPE), propertyType)) {
                    List<String> values;

                    if (property.getType() == Property.Type.ARRAY) {
                        values = inputParameters.getList(property.getName(), String.class, List.of());
                    } else {
                        String value = inputParameters.getString(property.getName());

                        values = value == null ? List.of() : List.of(value);
                    }

                    valuesMap.compute(property.getName(), (key, curValues) -> {
                        for (String value : values) {
                            if (StringUtils.isNotBlank(value)) {
                                if (curValues == null) {
                                    curValues = new ArrayList<>();
                                }

                                if (!curValues.contains(value)) {
                                    curValues.add(value);
                                }
                            }
                        }

                        return curValues;
                    });
                }
            }
        }

        return valuesMap;
    }
}
