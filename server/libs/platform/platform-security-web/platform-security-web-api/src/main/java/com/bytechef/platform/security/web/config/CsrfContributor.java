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

package com.bytechef.platform.security.web.config;

import java.util.List;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Defines an interface for contributing CSRF ignoring rules. Implementations of this interface specify a list of
 * request matchers that should be exempt from CSRF protection.
 *
 * This is useful in scenarios where certain API endpoints or request patterns need to bypass CSRF validation, such as
 * endpoints used for server-sent events, non-browser client interactions, or internal authenticated requests.
 *
 * @author Ivica Cardic
 */
public interface CsrfContributor {

    /**
     * Returns a list of {@link RequestMatcher}s that should be ignored for CSRF protection.
     *
     * @return a list of {@link RequestMatcher} instances representing the request patterns to be excluded from CSRF
     *         protection.
     */
    List<RequestMatcher> getIgnoringRequestMatchers();
}
