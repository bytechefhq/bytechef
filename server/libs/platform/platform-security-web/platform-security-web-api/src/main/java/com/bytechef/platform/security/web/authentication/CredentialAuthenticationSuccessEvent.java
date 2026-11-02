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

package com.bytechef.platform.security.web.authentication;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
public final class CredentialAuthenticationSuccessEvent extends ApplicationEvent {

    public CredentialAuthenticationSuccessEvent(Authentication authentication) {
        super(authentication);
    }

    public Authentication getAuthentication() {
        return (Authentication) getSource();
    }
}
