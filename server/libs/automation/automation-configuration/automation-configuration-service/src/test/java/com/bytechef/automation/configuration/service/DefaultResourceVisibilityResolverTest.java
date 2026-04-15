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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test class for {@link DefaultResourceVisibilityResolver}, the CE resolution: admin bypass, then reach, then
 * ownership. CE has no grants.
 */
class DefaultResourceVisibilityResolverTest {

    private final DefaultResourceVisibilityResolver resolver = new DefaultResourceVisibilityResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(String login, String... authorities) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    login, "password", List.of(authorities)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()));
    }

    @Test
    void testWorkspaceVisibleToEveryMember() {
        authenticate("ana");

        Set<Long> visibleIds = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.WORKSPACE, "ivica")));

        assertThat(visibleIds).containsExactly(10L);
    }

    @Test
    void testOrganizationVisibleToEveryMember() {
        authenticate("ana");

        Set<Long> visibleIds = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.ORGANIZATION, "ivica")));

        assertThat(visibleIds).containsExactly(10L);
    }

    @Test
    void testPrivateHiddenFromNonOwner() {
        authenticate("ana");

        Set<Long> visibleIds = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visibleIds).isEmpty();
    }

    @Test
    void testPrivateVisibleToOwner() {
        authenticate("ivica");

        Set<Long> visibleIds = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visibleIds).containsExactly(10L);
    }

    @Test
    void testPrivateVisibleToAdmin() {
        authenticate("marko", AuthorityConstants.ADMIN);

        Set<Long> visibleIds = resolver.filterVisibleIds(
            "Connection", 1L, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visibleIds).containsExactly(10L);
    }

    @Test
    void testMixedCandidatesAreFilteredIndependently() {
        authenticate("ana");

        Set<Long> visibleIds = resolver.filterVisibleIds(
            "Connection", 1L,
            List.of(
                new VisibilityRecord(10L, ResourceVisibility.WORKSPACE, "ivica"),
                new VisibilityRecord(11L, ResourceVisibility.PRIVATE, "ivica"),
                new VisibilityRecord(12L, ResourceVisibility.PRIVATE, "ana")));

        assertThat(visibleIds).containsExactlyInAnyOrder(10L, 12L);
    }

    @Test
    void testEmptyCandidateSetReturnsEmpty() {
        authenticate("ana");

        assertThat(resolver.filterVisibleIds("Connection", 1L, List.of())).isEmpty();
    }
}
