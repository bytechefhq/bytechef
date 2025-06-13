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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.JWT_TOKEN;
import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.PAYLOAD;
import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.SECRET;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Spehar
 */
public class JwtHelperVerifyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("verify")
        .title("Verify")
        .description("Verify JWT token.")
        .properties(
            string(JWT_TOKEN)
                .label("JWT Token")
                .description("JWT token you want to verify.")
                .required(true),
            string(SECRET)
                .label("Secret")
                .description("Secret of the JWT token.")
                .required(true))
        .output()
        .perform(JwtHelperVerifyAction::perform);

    private JwtHelperVerifyAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Algorithm algorithm = Algorithm.HMAC256(inputParameters.getRequiredString(SECRET));

        JWTVerifier verifier = JWT.require(algorithm)
            .build();

        DecodedJWT decodedJWT = verifier.verify(inputParameters.getRequiredString(JWT_TOKEN));

        return decodedJWT.getClaim(PAYLOAD)
            .asMap();
    }
}
