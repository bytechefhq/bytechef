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

package com.bytechef.mcp.server.security.web.filter;

import com.bytechef.platform.security.web.filter.AbstractPublicApiAuthenticationFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * @author Ivica Cardic
 */
public class ApiKeyAuthenticationFilter extends AbstractPublicApiAuthenticationFilter {

    @SuppressFBWarnings("EI")
    public ApiKeyAuthenticationFilter(AuthenticationManager authenticationManager) {
        super("^/api/v[0-9]+/mcp/.+", authenticationManager);
    }
}
