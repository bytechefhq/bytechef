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

package com.bytechef.platform.security.web.mcp.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.web.mcp.McpAuthenticationRequiredResolver;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Ivica Cardic
 */
class McpAuthenticationRequiredPredicateTest {

    private final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

    @Test
    void testRequiresAuthenticationWhenNoResolverContributed() {
        McpAuthenticationRequiredPredicate mcpAuthenticationRequiredPredicate =
            new McpAuthenticationRequiredPredicate(List.of());

        assertThat(mcpAuthenticationRequiredPredicate.test(mockHttpServletRequest)).isTrue();
    }

    @Test
    void testRequiresAuthenticationWhenNoResolverClaimsTheRequest() {
        McpAuthenticationRequiredPredicate mcpAuthenticationRequiredPredicate =
            new McpAuthenticationRequiredPredicate(List.of(request -> Optional.empty()));

        assertThat(mcpAuthenticationRequiredPredicate.test(mockHttpServletRequest)).isTrue();
    }

    @Test
    void testDoesNotRequireAuthenticationWhenResolverOptsOut() {
        McpAuthenticationRequiredPredicate mcpAuthenticationRequiredPredicate =
            new McpAuthenticationRequiredPredicate(List.of(request -> Optional.of(false)));

        assertThat(mcpAuthenticationRequiredPredicate.test(mockHttpServletRequest)).isFalse();
    }

    @Test
    void testFirstClaimingResolverWins() {
        McpAuthenticationRequiredResolver abstainingResolver = request -> Optional.empty();
        McpAuthenticationRequiredResolver optingOutResolver = request -> Optional.of(false);
        McpAuthenticationRequiredResolver requiringResolver = request -> Optional.of(true);

        McpAuthenticationRequiredPredicate mcpAuthenticationRequiredPredicate =
            new McpAuthenticationRequiredPredicate(
                List.of(abstainingResolver, optingOutResolver, requiringResolver));

        assertThat(mcpAuthenticationRequiredPredicate.test(mockHttpServletRequest)).isFalse();
    }
}
