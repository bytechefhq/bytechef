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

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.google.auto.service.AutoService;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;

/**
 * This class will not be overwritten on the repeated calls of the generator.
 *
 * @author Igor Beslic
 */
@AutoService(OpenApiComponentHandler.class)
public class GaurusComponentHandler extends AbstractGaurusComponentHandler {

    private static final String EMPTY_BODY = "";

    public ComponentDsl.ModifiableActionDefinition
        modifyAction(ComponentDsl.ModifiableActionDefinition modifiableActionDefinition) {

        Optional<Map<String, Object>> metadata = modifiableActionDefinition.getMetadata();

        Map<String, Object> metadataMap = metadata.orElseGet(Collections::emptyMap);

        String path = (String) metadataMap.get("path");

        modifiableActionDefinition.perform(new ActionDefinition.PerformFunction() {
            @Override
            public Object apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
                throws Exception {

                Optional<List<? extends Property>> propertiesOptional = modifiableActionDefinition.getProperties();

                String normalizedPath = path;
                if (propertiesOptional.isPresent()) {
                    List<? extends Property> properties = propertiesOptional.get();

                    for (Property property : properties) {
                        if (((PropertyType) property.getMetadata()
                            .get("type")) == PropertyType.PATH) {
                            normalizedPath = normalizedPath.replace(
                                "{" + property.getName() + "}",
                                URLEncoder.encode(inputParameters.getRequiredString(property.getName()),
                                    StandardCharsets.UTF_8));
                        }
                    }
                }

                String endpoint =
                    connectionParameters.getRequiredString("baseUri") + normalizedPath;

                Context.Http.Executor httpExecutor = context.http(http -> http.get(endpoint));

                String clientId = connectionParameters.getString("clientId");
                String clientSecret = connectionParameters.getString("clientSecret");
                String requestId = asUuid(context.getTraceId());
                String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()
                    .truncatedTo(ChronoUnit.MILLIS));

                String body = EMPTY_BODY;
                String signature =
                    getSignature(clientId, clientSecret, requestId, timestamp, getRelativeUrl(endpoint), body, context);

                context.log(log -> log.debug(
                    "Executing endpoint: {} with clientId: {}, requestId: {}, timestamp: {}, uri: {}, body: {}, signature {}",
                    endpoint, clientId, requestId, timestamp, path, body, signature));

                Context.Http.Response response = httpExecutor
                    .configuration(
                        Context.Http.allowUnauthorizedCerts(
                            connectionParameters.getBoolean("allowSelfSignedCert", false))
                            .responseType(
                                Context.Http.ResponseType.JSON))
                    .headers(Map.of("clientId", List.of(clientId), "requestId",
                        List.of(requestId), "timestamp", List.of(timestamp),
                        "signature", List.of(signature)))
                    .execute();

                return response.getBody();
            }
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
                        bool("allowSelfSignedCert")
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

            return Base64.getEncoder()
                .encodeToString(mac.doFinal(forSigning.getBytes(StandardCharsets.UTF_8)));
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

}
