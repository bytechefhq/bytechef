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

package com.bytechef.platform.user.web.security.config;

import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class UserAuthorizeHttpRequestContributor implements AuthorizeHttpRequestContributor {

    @Override
    public List<String> getApiPermitAllRequestMatcherPaths() {
        return List.of(
            "/api/activate", "/api/authenticate", "/api/account/reset-password/finish",
            "/api/account/reset-password/init", "/api/mfa/verify", "/api/register",
            "/api/send-activation-email", "/api/sso/discover", "/api/sso/discover-by-name",
            "/oauth2/authorization/**", "/login/oauth2/code/**");
    }
}
