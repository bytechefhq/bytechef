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

import java.util.List;

/**
 * Administers the OAuth2 clients registered with the embedded authorization server: listing the clients (created via
 * Dynamic Client Registration) and deleting one, which also revokes its authorizations.
 *
 * @author Ivica Cardic
 */
public interface RegisteredClientFacade {

    void deleteRegisteredClient(String id);

    List<RegisteredClientInfo> getRegisteredClients();
}
