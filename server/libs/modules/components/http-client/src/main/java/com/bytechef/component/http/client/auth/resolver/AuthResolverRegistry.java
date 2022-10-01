/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.http.client.auth.resolver;

import com.bytechef.component.http.client.constants.HttpClientConstants;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class AuthResolverRegistry {

    private static final Map<HttpClientConstants.AuthType, AuthResolver> HTTP_AUTH_MAP = new HashMap<>() {
        {
            put(HttpClientConstants.AuthType.API_KEY, new ApiKeyAuthResolver());
            put(HttpClientConstants.AuthType.BASIC_AUTH, new BasicAuthResolver());
            put(HttpClientConstants.AuthType.BEARER_TOKEN, new BearerTokenAuthResolver());
            put(HttpClientConstants.AuthType.DIGEST_AUTH, new DigestAuthResolver());
            put(HttpClientConstants.AuthType.OAUTH2, new OAuth2AuthResolver());
        }
    };

    public static AuthResolver get(HttpClientConstants.AuthType authType) {
        return HTTP_AUTH_MAP.get(authType);
    }
}
