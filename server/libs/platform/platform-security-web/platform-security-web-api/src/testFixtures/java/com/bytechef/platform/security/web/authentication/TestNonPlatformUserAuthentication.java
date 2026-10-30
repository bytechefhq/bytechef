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

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * @author Ivica Cardic
 */
public final class TestNonPlatformUserAuthentication extends AbstractAuthenticationToken
    implements NonPlatformUserAuthentication {

    private final String name;

    public TestNonPlatformUserAuthentication(String name, boolean authenticated) {
        super(List.of());

        this.name = name;

        setAuthenticated(authenticated);
    }

    public static TestNonPlatformUserAuthentication of(String name) {
        return new TestNonPlatformUserAuthentication(name, true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return name;
    }
}
