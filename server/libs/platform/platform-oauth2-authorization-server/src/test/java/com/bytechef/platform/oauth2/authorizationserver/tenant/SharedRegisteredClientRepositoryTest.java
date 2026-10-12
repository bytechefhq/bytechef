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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * @author Ivica Cardic
 */
class SharedRegisteredClientRepositoryTest {

    private final RegisteredClientRepository delegate = mock(RegisteredClientRepository.class);
    private final SharedRegisteredClientRepository repository = new SharedRegisteredClientRepository(delegate);

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testAllOperationsRunInDefaultTenant() {
        List<String> seenTenantIds = new ArrayList<>();

        doAnswer(invocation -> seenTenantIds.add(TenantContext.getCurrentTenantId())).when(delegate)
            .save(any());
        when(delegate.findById(anyString())).thenAnswer(invocation -> {
            seenTenantIds.add(TenantContext.getCurrentTenantId());

            return null;
        });
        when(delegate.findByClientId(anyString())).thenAnswer(invocation -> {
            seenTenantIds.add(TenantContext.getCurrentTenantId());

            return null;
        });

        TenantContext.setCurrentTenantId("acme");

        repository.save(RegisteredClient.withId("id")
            .clientId("client")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/cb")
            .build());
        repository.findById("id");
        repository.findByClientId("client");

        assertThat(seenTenantIds).containsExactly(
            TenantContext.DEFAULT_TENANT_ID, TenantContext.DEFAULT_TENANT_ID, TenantContext.DEFAULT_TENANT_ID);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("acme");
    }
}
