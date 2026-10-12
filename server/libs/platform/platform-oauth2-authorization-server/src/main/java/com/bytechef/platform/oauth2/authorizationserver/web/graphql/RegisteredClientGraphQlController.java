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

package com.bytechef.platform.oauth2.authorizationserver.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.oauth2.authorizationserver.annotation.ConditionalOnOauth2AuthorizationServerEnabled;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2AuthorizationGrant;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2ConsentClient;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import com.bytechef.platform.oauth2.authorizationserver.facade.RegisteredClientFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for registered OAuth2 clients and their per-user grants.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnOauth2AuthorizationServerEnabled
public class RegisteredClientGraphQlController {

    private final RegisteredClientFacade registeredClientFacade;

    @SuppressFBWarnings("EI")
    public RegisteredClientGraphQlController(RegisteredClientFacade registeredClientFacade) {
        this.registeredClientFacade = registeredClientFacade;
    }

    @MutationMapping
    public boolean deleteRegisteredClient(@Argument String id) {
        registeredClientFacade.deleteRegisteredClient(id);

        return true;
    }

    @QueryMapping
    @Nullable
    public OAuth2ConsentClient oauth2ConsentClient(@Argument String clientId) {
        return registeredClientFacade.fetchConsentClient(clientId)
            .orElse(null);
    }

    @QueryMapping
    public List<OAuth2AuthorizationGrant> registeredClientGrants(@Argument String registeredClientId) {
        return registeredClientFacade.getRegisteredClientGrants(registeredClientId);
    }

    @QueryMapping
    public List<RegisteredClientInfo> registeredClients() {
        return registeredClientFacade.getRegisteredClients();
    }

    @MutationMapping
    public boolean revokeRegisteredClientGrant(@Argument String registeredClientId, @Argument String principalName) {
        registeredClientFacade.revokeRegisteredClientGrant(registeredClientId, principalName);

        return true;
    }

    @MutationMapping
    public boolean revokeRegisteredClientGrants(@Argument String registeredClientId) {
        registeredClientFacade.revokeRegisteredClientGrants(registeredClientId);

        return true;
    }
}
