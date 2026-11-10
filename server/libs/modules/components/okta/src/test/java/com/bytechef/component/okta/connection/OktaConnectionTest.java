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

package com.bytechef.component.okta.connection;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.okta.constant.OktaConstants.DOMAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyFunction;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ConnectionDefinition.BaseUriFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class OktaConnectionTest {

    @Test
    void testBaseUri() throws Exception {
        BaseUriFunction baseUriFunction = OktaConnection.CONNECTION_DEFINITION.getBaseUri()
            .orElseThrow();

        Parameters connectionParameters = MockParametersFactory.create(
            Map.of(DOMAIN, "https://your-org.okta.com/"));

        assertEquals("https://your-org.okta.com/api/v1", baseUriFunction.apply(connectionParameters, null));
    }

    @Test
    void testApply() throws Exception {
        List<? extends Authorization> authorizations = OktaConnection.CONNECTION_DEFINITION.getAuthorizations();

        Authorization authorization = authorizations.getFirst();

        ApplyFunction applyFunction = authorization.getApply()
            .orElseThrow();

        Parameters connectionParameters = MockParametersFactory.create(
            Map.of(DOMAIN, "your-org.okta.com", TOKEN, "token1"));

        ApplyResponse applyResponse = applyFunction.apply(connectionParameters, null);

        assertEquals(Map.of(AUTHORIZATION, List.of("SSWS token1")), applyResponse.getHeaders());
    }
}
