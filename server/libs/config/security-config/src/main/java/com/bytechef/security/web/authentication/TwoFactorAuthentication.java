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

package com.bytechef.security.web.authentication;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Wrapper token that holds the primary (password-verified) authentication while the user completes TOTP verification.
 * Returns {@code isAuthenticated() == false} and empty authorities until the second factor is verified.
 *
 * @author Ivica Cardic
 */
public class TwoFactorAuthentication extends AbstractAuthenticationToken {

    private final Authentication primary;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public TwoFactorAuthentication(Authentication primary) {
        super(Collections.emptyList());

        this.primary = primary;
    }

    @Override
    public Object getCredentials() {
        return primary.getCredentials();
    }

    @Override
    public Object getPrincipal() {
        return primary.getPrincipal();
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Authentication getPrimary() {
        return primary;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }
}
