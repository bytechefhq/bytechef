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

package com.bytechef.platform.oauth2.authorizationserver.token;

import com.bytechef.platform.oauth2.authorizationserver.registration.AppClients;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * @author Ivica Cardic
 */
public class AppUserIdTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

    private final ObjectProvider<UserService> userServiceProvider;

    @SuppressFBWarnings("EI")
    public AppUserIdTokenCustomizer(ObjectProvider<UserService> userServiceProvider) {
        this.userServiceProvider = userServiceProvider;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (!ID_TOKEN_TOKEN_TYPE.equals(context.getTokenType())
            || !AppClients.isAppClient(context.getRegisteredClient())) {
            return;
        }

        Authentication principal = context.getPrincipal();
        UserService userService = userServiceProvider.getIfAvailable();

        if (principal == null || userService == null) {
            return;
        }

        Optional<User> userOptional = userService.fetchUserByLogin(principal.getName());

        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();

        JwtClaimsSet.Builder claims = context.getClaims();

        String name = getName(user);

        if (name != null) {
            claims.claim(StandardClaimNames.NAME, name);
        }

        String email = user.getEmail();

        if (email != null && !email.isBlank()) {
            claims.claim(StandardClaimNames.EMAIL, email);
        }
    }

    @Nullable
    private static String getName(User user) {
        String name = Stream.of(user.getFirstName(), user.getLastName())
            .filter(namePart -> namePart != null && !namePart.isBlank())
            .map(String::strip)
            .collect(Collectors.joining(" "));

        return name.isEmpty() ? null : name;
    }
}
