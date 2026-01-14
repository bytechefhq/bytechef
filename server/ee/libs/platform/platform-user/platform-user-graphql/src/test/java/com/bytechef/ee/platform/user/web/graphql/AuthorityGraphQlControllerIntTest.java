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

package com.bytechef.ee.platform.user.web.graphql;

import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.web.graphql.config.PlatformUserGraphQlConfigurationSharedMocks;
import com.bytechef.ee.platform.user.web.graphql.config.PlatformUserGraphQlTestConfiguration;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    PlatformUserGraphQlTestConfiguration.class,
    AuthorityGraphQlController.class
})
@GraphQlTest(
    controllers = AuthorityGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@PlatformUserGraphQlConfigurationSharedMocks
public class AuthorityGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private AuthorityService authorityService;

    @Test
    void testGetAuthorities() {
        // Given
        Authority adminAuthority = createMockAuthority(1L, "ROLE_ADMIN");
        Authority userAuthority = createMockAuthority(2L, "ROLE_USER");

        when(authorityService.getAuthorities()).thenReturn(List.of(adminAuthority, userAuthority));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    authorities
                }
                """)
            .execute()
            .path("authorities")
            .entityList(String.class)
            .hasSize(2)
            .contains("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void testGetAuthoritiesEmpty() {
        // Given
        when(authorityService.getAuthorities()).thenReturn(List.of());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    authorities
                }
                """)
            .execute()
            .path("authorities")
            .entityList(String.class)
            .hasSize(0);
    }

    private Authority createMockAuthority(Long id, String name) {
        Authority authority = new Authority();

        authority.setId(id);
        authority.setName(name);

        return authority;
    }
}
