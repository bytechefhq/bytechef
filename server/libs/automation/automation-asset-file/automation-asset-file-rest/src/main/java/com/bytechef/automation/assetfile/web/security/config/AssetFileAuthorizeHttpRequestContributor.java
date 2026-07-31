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

package com.bytechef.automation.assetfile.web.security.config;

import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Permits anonymous access to the token-gated asset file download endpoints. Access control on these paths is the
 * capability token itself (durable public-link token or short-lived HMAC-signed token) — see
 * {@code AssetFilePublicDownloadController}.
 *
 * @author Ivica Cardic
 */
@Component
public class AssetFileAuthorizeHttpRequestContributor implements AuthorizeHttpRequestContributor {

    @Override
    public List<String> getApiPermitAllRequestMatcherPaths() {
        return List.of(
            "/api/automation/asset-files/public/**",
            "/api/automation/asset-files/signed/**");
    }
}
