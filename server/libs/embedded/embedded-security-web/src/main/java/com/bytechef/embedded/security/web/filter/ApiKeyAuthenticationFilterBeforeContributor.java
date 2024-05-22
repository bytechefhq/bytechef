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

import com.bytechef.platform.security.web.filter.FilterBeforeContributor;
import com.bytechef.platform.user.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.Filter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ApiKeyAuthenticationFilterBeforeContributor implements FilterBeforeContributor {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @SuppressFBWarnings("EI")
    public ApiKeyAuthenticationFilterBeforeContributor(ApiKeyService apiKeyService) {
        this.apiKeyAuthenticationFilter = new ApiKeyAuthenticationFilter(apiKeyService);
    }

    @Override
    @SuppressFBWarnings("EI")
    public Filter getFilter() {
        return apiKeyAuthenticationFilter;
    }

    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return BasicAuthenticationFilter.class;
    }
}
