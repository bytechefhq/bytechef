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

package com.bytechef.component.jwt.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.KEY;
import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.PAYLOAD;
import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.SECRET;
import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.VALUE;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class JwtHelperSignAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sign")
        .title("Sign")
        .description("Creates JWT token.")
        .properties(
            array(PAYLOAD)
                .label("Payload")
                .description("Payload of the JWT token.")
                .required(true)
                .items(
                    object("item")
                        .description("One key-value pair of payload object.")
                        .properties(
                            string(KEY)
                                .label("Key")
                                .description("Key of the payload object.")
                                .required(true),
                            string(VALUE)
                                .label("Value")
                                .description("Value of the payload object.")
                                .required(true))),
            string(SECRET)
                .label("Secret")
                .description("Secret of the JWT token.")
                .required(true))
        .output(
            outputSchema(
                string()
                    .description("JWT token that was created form the payload.")))
        .perform(JwtHelperSignAction::perform);

    private JwtHelperSignAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Algorithm algorithm = Algorithm.HMAC256(inputParameters.getRequiredString(SECRET));

        return JWT.create()
            .withClaim(PAYLOAD, getPayload(inputParameters.getList(PAYLOAD, new TypeReference<>() {})))
            .sign(algorithm);
    }

    private static Map<String, String> getPayload(List<Map<String, String>> payloads) {
        Map<String, String> payloadMap = new HashMap<>();

        for (Map<String, String> payloadObject : payloads) {
            payloadMap.put(payloadObject.get(KEY), payloadObject.get(VALUE));
        }

        return payloadMap;
    }
}
