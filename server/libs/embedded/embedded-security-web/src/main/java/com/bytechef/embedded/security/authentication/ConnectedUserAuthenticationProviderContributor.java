/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.security.authentication;

import com.bytechef.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.security.web.authentication.AuthenticationProviderContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectedUserAuthenticationProviderContributor implements AuthenticationProviderContributor {

    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationProviderContributor(ConnectedUserService connectedUserService) {
        this.connectedUserService = connectedUserService;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return new ConnectedUserAuthenticationProvider(connectedUserService);
    }
}
