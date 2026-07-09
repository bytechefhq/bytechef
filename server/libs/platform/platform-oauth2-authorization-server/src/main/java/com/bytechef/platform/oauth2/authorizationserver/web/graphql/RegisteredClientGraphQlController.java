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
import com.bytechef.platform.oauth2.authorizationserver.facade.RegisteredClientFacade;
import com.bytechef.platform.oauth2.authorizationserver.facade.RegisteredClientInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing the admin surface for OAuth2 clients registered with the embedded authorization server:
 * listing the DCR-created clients and deleting one. Authorization is enforced on {@link RegisteredClientFacade}.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class RegisteredClientGraphQlController {

    private final RegisteredClientFacade registeredClientFacade;

    @SuppressFBWarnings("EI")
    public RegisteredClientGraphQlController(RegisteredClientFacade registeredClientFacade) {
        this.registeredClientFacade = registeredClientFacade;
    }

    @QueryMapping(name = "registeredClients")
    public List<RegisteredClientInfo> registeredClients() {
        return registeredClientFacade.getRegisteredClients();
    }

    @MutationMapping(name = "deleteRegisteredClient")
    public Boolean deleteRegisteredClient(@Argument String id) {
        registeredClientFacade.deleteRegisteredClient(id);

        return true;
    }
}
