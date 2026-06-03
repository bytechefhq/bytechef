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
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.google.auto.service.AutoService;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * This class will not be overwritten on the subsequent calls of the generator.
 *
 * @author Igor Beslic
 */
@AutoService(OpenApiComponentHandler.class)
public class GaurusComponentHandler extends AbstractGaurusComponentHandler {

    public ComponentDsl.ModifiableActionDefinition
        modifyAction(ComponentDsl.ModifiableActionDefinition modifiableActionDefinition) {
        if (Objects.equals("getExternalUsers", modifiableActionDefinition.getName())) {
            Optional<Map<String, Object>> metadata = modifiableActionDefinition.getMetadata();

            Map<String, Object> metadataMap = metadata.orElseGet(Collections::emptyMap);

            String path = (String) metadataMap.get("path");

            modifiableActionDefinition.perform(new ActionDefinition.PerformFunction() {
                @Override
                public Object apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
                    throws Exception {
                    String endpoint =
                        connectionParameters.getRequiredString("baseUri") + path;

                    Context.Http.Executor httpExecutor = context.http(http -> http.get(endpoint));

                    String clientId = connectionParameters.getString("clientId");
                    String clientSecret = connectionParameters.getString("clientSecret");
                    String requestId = "SHOUD-GENERATE-0001";
                    String timestamp =
                        DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(System.currentTimeMillis());

                    Context.Http.Response response = httpExecutor
                        .configuration(Context.Http
                            .allowUnauthorizedCerts(connectionParameters.getBoolean("allowSelfSignedCert", false)))
                        .headers(Map.of("clienId", List.of(clientId), "requestId",
                            List.of(requestId), "timestamp", List.of(timestamp),
                            "signature", List.of(getSignature(clientId, clientSecret, requestId, timestamp, endpoint))))
                        .execute();

                    return response.getBody();
                }
            });
        }

        return modifiableActionDefinition;
    }

    private static String getSignature(
        String clientId, String clientSecret, String requestId, String timestamp, String uri) {

        String forSigning = String.format("%s;%s;%s;%s;", clientId, requestId, timestamp, uri);

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            return Base64.getEncoder()
                .encodeToString(mac.doFinal(forSigning.getBytes(StandardCharsets.UTF_8)));
        } catch (InvalidKeyException | NoSuchAlgorithmException exception) {
            throw new RuntimeException("Failed to compute HmacSHA256 signature", exception);
        }
    }
}
