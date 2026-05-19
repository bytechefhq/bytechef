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

package com.bytechef.platform.connection.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.connection.web.graphql.dto.ConnectionCredentialStoreInfo;
import com.bytechef.platform.credential.store.CredentialStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing which {@link CredentialStore} backends are registered in the current deployment. Consumed
 * by the UI to render the store picker on connection creation forms.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ConnectionCredentialStoreGraphQlController {

    private final List<CredentialStore> credentialStores;

    @SuppressFBWarnings("EI2")
    public ConnectionCredentialStoreGraphQlController(List<CredentialStore> credentialStores) {
        this.credentialStores = credentialStores;
    }

    @QueryMapping
    public List<ConnectionCredentialStoreInfo> connectionCredentialStores() {
        return credentialStores.stream()
            .map(store -> new ConnectionCredentialStoreInfo(store.getType(), store.isReadOnly()))
            .toList();
    }
}
