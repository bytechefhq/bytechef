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

package com.bytechef.platform.oauth2.authorizationserver.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class OAuth2AuthorizationGrantTest {

    private static final Instant AUTHORIZED_AT = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant LAST_ISSUED_AT = Instant.parse("2026-10-02T00:00:00Z");

    @Test
    void testGrantWithoutIssueTimesIsAllowed() {
        OAuth2AuthorizationGrant grant = new OAuth2AuthorizationGrant(
            "alice", List.of("mcp:automation"), List.of("Management"), null, null, null);

        assertThat(grant.authorizedAt()).isNull();
        assertThat(grant.lastIssuedAt()).isNull();
    }

    @Test
    void testPrincipalNameIsRequired() {
        assertThatNullPointerException().isThrownBy(
            () -> new OAuth2AuthorizationGrant(null, List.of(), List.of(), AUTHORIZED_AT, LAST_ISSUED_AT, null));
    }

    @Test
    void testIssueTimesMustBothBePresentOrBothAbsent() {
        assertThatIllegalArgumentException().isThrownBy(
            () -> new OAuth2AuthorizationGrant("alice", List.of(), List.of(), AUTHORIZED_AT, null, null));
        assertThatIllegalArgumentException().isThrownBy(
            () -> new OAuth2AuthorizationGrant("alice", List.of(), List.of(), null, LAST_ISSUED_AT, null));
    }

    @Test
    void testLastIssuedAtCannotPrecedeAuthorizedAt() {
        assertThatIllegalArgumentException().isThrownBy(
            () -> new OAuth2AuthorizationGrant("alice", List.of(), List.of(), LAST_ISSUED_AT, AUTHORIZED_AT, null));
    }
}
