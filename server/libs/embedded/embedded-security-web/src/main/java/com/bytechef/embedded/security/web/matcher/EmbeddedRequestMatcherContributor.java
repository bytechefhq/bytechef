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

package com.bytechef.embedded.security.web.matcher;

import com.bytechef.platform.security.web.matcher.RequestMatcherContributor;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class EmbeddedRequestMatcherContributor implements RequestMatcherContributor {

    @Override
    public RequestMatcher[] getRequestMatcher(MvcRequestMatcher.Builder mvc) {
        return new RequestMatcher[] {
            mvc.pattern("/api/embedded/public/**"),
            mvc.pattern("/api/embedded/by-connected-user-token/**")
        };
    }
}
