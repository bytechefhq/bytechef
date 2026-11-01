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

package com.bytechef.platform.oauth2.authorizationserver.facade;

import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.oauth2.authorizationserver.annotation.ConditionalOnOauth2AuthorizationServerEnabled;
import com.bytechef.platform.oauth2.authorizationserver.audit.OAuth2ClientAuditEvents;
import com.bytechef.platform.oauth2.authorizationserver.audit.OAuth2ClientAuditMapper;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2AuthorizationGrant;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2ConsentClient;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnOauth2AuthorizationServerEnabled
public class RegisteredClientFacadeImpl implements RegisteredClientFacade {

    private final OAuth2AuthorizationGrantService oAuth2AuthorizationGrantService;
    private final RegisteredClientService registeredClientService;

    @SuppressFBWarnings("EI2")
    public RegisteredClientFacadeImpl(
        OAuth2AuthorizationGrantService oAuth2AuthorizationGrantService,
        RegisteredClientService registeredClientService) {

        this.oAuth2AuthorizationGrantService = oAuth2AuthorizationGrantService;
        this.registeredClientService = registeredClientService;
    }

    @Override
    @Audited(
        event = OAuth2ClientAuditEvents.OAUTH2_CLIENT_DELETED, mapper = OAuth2ClientAuditMapper.class, data = {
            @AuditData(key = "registeredClientId", value = "#id")
        })
    @PreAuthorize("isTenantAdmin()")
    public boolean deleteRegisteredClient(String id) {
        return registeredClientService.deleteRegisteredClient(id);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Optional<OAuth2ConsentClient> fetchConsentClient(String clientId) {
        return registeredClientService.fetchConsentClient(clientId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<OAuth2AuthorizationGrant> getRegisteredClientGrants(String registeredClientId) {
        return oAuth2AuthorizationGrantService.getGrants(registeredClientId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<RegisteredClientInfo> getRegisteredClients() {
        return registeredClientService.getRegisteredClients();
    }

    @Override
    @Audited(event = OAuth2ClientAuditEvents.OAUTH2_CLIENT_GRANT_REVOKED, data = {
        @AuditData(key = "registeredClientId", value = "#registeredClientId"),
        @AuditData(key = "principalName", value = "#principalName")
    })
    @PreAuthorize("isTenantAdmin()")
    public void revokeRegisteredClientGrant(String registeredClientId, String principalName) {
        oAuth2AuthorizationGrantService.revokeGrant(registeredClientId, principalName);
    }

    @Override
    @Audited(event = OAuth2ClientAuditEvents.OAUTH2_CLIENT_ALL_GRANTS_REVOKED, data = {
        @AuditData(key = "registeredClientId", value = "#registeredClientId")
    })
    @PreAuthorize("isTenantAdmin()")
    public void revokeRegisteredClientGrants(String registeredClientId) {
        oAuth2AuthorizationGrantService.revokeGrants(registeredClientId);
    }
}
