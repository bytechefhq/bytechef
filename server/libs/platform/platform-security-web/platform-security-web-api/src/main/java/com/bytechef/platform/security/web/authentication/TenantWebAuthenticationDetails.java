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

import jakarta.servlet.http.HttpServletRequest;
import java.io.Serial;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * @author Ivica Cardic
 */
public class TenantWebAuthenticationDetails extends WebAuthenticationDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final @Nullable String tenantId;

    public TenantWebAuthenticationDetails(HttpServletRequest request, @Nullable String tenantId) {
        super(request);

        this.tenantId = tenantId;
    }

    public TenantWebAuthenticationDetails(
        String remoteAddress, @Nullable String sessionId, @Nullable String tenantId) {

        super(remoteAddress, sessionId);

        this.tenantId = tenantId;
    }

    public @Nullable String getTenantId() {
        return tenantId;
    }

    public TenantWebAuthenticationDetails withTenantId(String tenantId) {
        return new TenantWebAuthenticationDetails(getRemoteAddress(), getSessionId(), tenantId);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (!super.equals(object)) {
            return false;
        }

        return object instanceof TenantWebAuthenticationDetails tenantWebAuthenticationDetails &&
            Objects.equals(tenantId, tenantWebAuthenticationDetails.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tenantId);
    }

    @Override
    public String toString() {
        return "TenantWebAuthenticationDetails [RemoteIpAddress=" + getRemoteAddress() + ", SessionId=" +
            getSessionId() + ", TenantId=" + tenantId + "]";
    }
}
