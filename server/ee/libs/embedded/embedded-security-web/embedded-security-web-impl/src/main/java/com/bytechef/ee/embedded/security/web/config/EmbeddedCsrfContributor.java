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

package com.bytechef.ee.embedded.security.web.config;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.platform.security.web.config.CsrfContributor;
import java.util.List;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class EmbeddedCsrfContributor implements CsrfContributor {

    @Override
    public List<RequestMatcher> getIgnoringRequestMatchers() {
        return List.of(
            regexMatcher("^/api/embedded/v[0-9]+/.+"),
            // For internal calls from the embedded workflow builder
            request -> request.getHeader("Authorization") != null);
    }
}
