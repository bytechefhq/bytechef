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

package com.bytechef.platform.oauth2.authorizationserver.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * @author Ivica Cardic
 */
class TenantScopeTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testCallRestoresPreviousTenant() {
        TenantContext.setCurrentTenantId("outer");

        String inner = TenantScope.call("inner", TenantContext::getCurrentTenantId);

        assertThat(inner).isEqualTo("inner");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("outer");
    }

    @Test
    void testCallRethrowsRuntimeExceptionUnwrapped() {
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException("invalid_grant");

        assertThatThrownBy(() -> TenantScope.call("inner", () -> {
            throw exception;
        })).isSameAs(exception);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRunRestoresPreviousTenant() {
        TenantContext.setCurrentTenantId("outer");

        String[] seenTenantId = new String[1];

        TenantScope.run("inner", () -> seenTenantId[0] = TenantContext.getCurrentTenantId());

        assertThat(seenTenantId[0]).isEqualTo("inner");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("outer");
    }
}
