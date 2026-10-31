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

package com.bytechef.platform.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class NonPlatformUserAuthenticationTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsCurrentIsTrueOnlyForAnAuthenticatedNonPlatformUserToken() {
        assertThat(NonPlatformUserAuthentication.isCurrent()).isFalse();

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of()));

        assertThat(NonPlatformUserAuthentication.isCurrent()).isFalse();

        SecurityContextHolder.getContext()
            .setAuthentication(new TestNonPlatformUserAuthentication("external-user", false));

        assertThat(NonPlatformUserAuthentication.isCurrent()).isFalse();

        SecurityContextHolder.getContext()
            .setAuthentication(TestNonPlatformUserAuthentication.of("external-user"));

        assertThat(NonPlatformUserAuthentication.isCurrent()).isTrue();
    }
}
