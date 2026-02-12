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

package com.bytechef.platform.user.domain;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("identity_provider_domain")
public class IdentityProviderDomain {

    @Column
    private String domain;

    @Id
    private Long id;

    @Column("identity_provider_id")
    private Long identityProviderId;

    public IdentityProviderDomain() {
    }

    public IdentityProviderDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public Long getId() {
        return id;
    }

    public Long getIdentityProviderId() {
        return identityProviderId;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIdentityProviderId(Long identityProviderId) {
        this.identityProviderId = identityProviderId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof IdentityProviderDomain identityProviderDomain)) {
            return false;
        }

        return Objects.equals(id, identityProviderDomain.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "IdentityProviderDomain{" +
            "id=" + id +
            ", domain='" + domain + '\'' +
            ", identityProviderId=" + identityProviderId +
            '}';
    }
}
