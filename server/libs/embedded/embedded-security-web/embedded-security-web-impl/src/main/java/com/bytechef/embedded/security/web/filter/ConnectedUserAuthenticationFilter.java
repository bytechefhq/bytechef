/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.security.web.filter;

import com.bytechef.embedded.security.web.authentication.ConnectedUserAuthenticationToken;
import com.bytechef.platform.security.web.filter.AbstractPublicApiAuthenticationFilter;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.Validate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
public class ConnectedUserAuthenticationFilter extends AbstractPublicApiAuthenticationFilter {

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationFilter(AuthenticationManager authenticationManager) {
        super("^/api/embedded/v[0-9]+/(?!frontend/).+", authenticationManager);

    }

    @Override
    protected Authentication getAuthentication(HttpServletRequest request) {
        String token = getAuthToken(request);

        TenantKey tenantKey = TenantKey.parse(token);

        Validate.notNull(request.getParameterValues("externalUserId"), "externalUserId parameter is required");

        return new ConnectedUserAuthenticationToken(
            request.getParameterValues("externalUserId")[0], getEnvironment(request), tenantKey.getTenantId());
    }
}
