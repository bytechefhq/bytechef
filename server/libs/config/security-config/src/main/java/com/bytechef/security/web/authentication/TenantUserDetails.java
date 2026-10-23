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

import java.util.Collection;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * A {@link User} that also records the tenant whose user record was authenticated.
 *
 * @author Ivica Cardic
 */
public final class TenantUserDetails extends User {

    private final String tenantId;

    public TenantUserDetails(
        String username, String password, Collection<? extends GrantedAuthority> authorities, String tenantId) {

        super(username, password, authorities);

        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof TenantUserDetails tenantUserDetails)) {
            return false;
        }

        return super.equals(tenantUserDetails) && tenantId.equals(tenantUserDetails.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tenantId);
    }
}
