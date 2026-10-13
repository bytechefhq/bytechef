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

package com.bytechef.platform.component.domain;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AuthorizationTest {

    @Test
    void testRefreshOnDefaultsToUnauthorized() {
        ModifiableAuthorization authorization = authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        assertThat(authorization.getRefreshOn()).isEqualTo(List.of(401));
    }

    @Test
    void testIsDiscoveryWhenProtectedResourceUrlIsDeclared() {
        Authorization authorization = new Authorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .protectedResourceUrl(
                    (connectionParameters, context) -> connectionParameters.getRequiredString("url")));

        assertThat(authorization.isDiscovery()).isTrue();
    }

    @Test
    void testIsNotDiscoveryWithoutProtectedResourceUrl() {
        Authorization authorization = new Authorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        assertThat(authorization.isDiscovery()).isFalse();
    }

    @Test
    void testOAuth2AuthorizationParametersFourArgumentConstructorLeavesContextNull() {
        OAuth2AuthorizationParameters parameters = new OAuth2AuthorizationParameters(
            "https://auth.example.com/authorize", "client", Map.of(), Map.of());

        assertThat(parameters.authorizationContext()).isNull();
    }
}
