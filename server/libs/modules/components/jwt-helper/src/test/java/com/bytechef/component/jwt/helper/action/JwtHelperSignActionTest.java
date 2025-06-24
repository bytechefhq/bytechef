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

import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.KEY;
import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.PAYLOAD;
import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.SECRET;
import static com.bytechef.component.jwt.helper.constant.JwtHelperConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class JwtHelperSignActionTest {

    @Test
    void testPerform() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                SECRET, "testSecret", PAYLOAD, List.of(
                    Map.of(KEY, "key1", VALUE, "value1"),
                    Map.of(KEY, "key2", VALUE, "value2"))));

        String jwtToken = JwtHelperSignAction.perform(mockedParameters, mockedParameters, mock(Context.class));

        Algorithm algorithm = Algorithm.HMAC256("testSecret");

        Claim claim = JWT.require(algorithm)
            .build()
            .verify(jwtToken)
            .getClaim(PAYLOAD);

        Map<String, Object> expectedPayload = Map.of("key1", "value1", "key2", "value2");

        assertEquals(expectedPayload, claim.asMap());
    }
}
