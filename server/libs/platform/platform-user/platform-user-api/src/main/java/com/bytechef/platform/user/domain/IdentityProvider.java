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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("identity_provider")
public class IdentityProvider {

    @Column("auto_provision")
    private boolean autoProvision;

    @Column("client_id")
    private String clientId;

    @Column("client_secret")
    private String clientSecret;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @Column("default_authority")
    private String defaultAuthority = "ROLE_USER";

    @MappedCollection(idColumn = "identity_provider_id")
    private Set<IdentityProviderDomain> domains;

    @Column
    private boolean enabled;

    @Column
    private boolean enforced;

    @Id
    private Long id;

    @Column("issuer_uri")
    private String issuerUri;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column
    private String scopes = "openid,profile,email";

    @Column
    private String type = "OIDC";

    public boolean isAutoProvision() {
        return autoProvision;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getDefaultAuthority() {
        return defaultAuthority;
    }

    @SuppressFBWarnings("EI")
    public Set<IdentityProviderDomain> getDomains() {
        return domains;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEnforced() {
        return enforced;
    }

    public Long getId() {
        return id;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public String getScopes() {
        return scopes;
    }

    public String getType() {
        return type;
    }

    public void setAutoProvision(boolean autoProvision) {
        this.autoProvision = autoProvision;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setDefaultAuthority(String defaultAuthority) {
        this.defaultAuthority = defaultAuthority;
    }

    @SuppressFBWarnings("EI2")
    public void setDomains(Set<IdentityProviderDomain> domains) {
        this.domains = domains;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnforced(boolean enforced) {
        this.enforced = enforced;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof IdentityProvider identityProvider)) {
            return false;
        }

        return Objects.equals(id, identityProvider.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "IdentityProvider{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", issuerUri='" + issuerUri + '\'' +
            ", enabled=" + enabled +
            ", enforced=" + enforced +
            ", autoProvision=" + autoProvision +
            '}';
    }
}
