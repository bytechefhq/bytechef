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

import com.bytechef.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.embedded.security.web.filter.ConnectedUserAuthenticationToken;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
public class ConnectedUserAuthenticationProvider implements AuthenticationProvider {

    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationProvider(ConnectedUserService connectedUserService) {
        this.connectedUserService = connectedUserService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ConnectedUserAuthenticationToken connectedUserAuthenticationToken =
            (ConnectedUserAuthenticationToken) authentication;

        Environment environment = connectedUserAuthenticationToken.getEnvironment();
        String externalUserId = connectedUserAuthenticationToken.getExternalUserId();

        ConnectedUser connectedUser = connectedUserService.fetchConnectedUser(environment, externalUserId)
            .orElseGet(() -> connectedUserService.createConnectedUser(environment, externalUserId));

        return new ConnectedUserAuthenticationToken(createSpringSecurityUser(externalUserId, connectedUser));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ConnectedUserAuthenticationToken.class);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(
        String externalUserId, ConnectedUser connectedUser) {

        if (!connectedUser.isEnabled()) {
            throw new UserNotActivatedException("Connected User " + externalUserId + " was not enabled");
        }

        return new org.springframework.security.core.userdetails.User(connectedUser.getExternalId(), "", List.of());
    }
}
