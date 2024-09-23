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

package com.bytechef.platform.security.web.authentication;

import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractPublicApiAuthenticationToken extends AbstractAuthenticationToken {

    private Environment environment;
    private String tenantId;
    private AppType type;
    private User user;

    public AbstractPublicApiAuthenticationToken(Environment environment, String tenantId, AppType type) {
        super(List.of());

        this.environment = environment;
        this.tenantId = tenantId;
        this.type = type;
    }

    @SuppressFBWarnings("EI")
    public AbstractPublicApiAuthenticationToken(User user) {
        super(user.getAuthorities());

        this.user = user;

        setAuthenticated(true);
        setDetails(user);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    public Environment getEnvironment() {
        return environment;
    }

    @Override
    @SuppressFBWarnings("EI")
    public Object getPrincipal() {
        return user;
    }

    public String getTenantId() {
        return tenantId;
    }

    public AppType getType() {
        return type;
    }
}
