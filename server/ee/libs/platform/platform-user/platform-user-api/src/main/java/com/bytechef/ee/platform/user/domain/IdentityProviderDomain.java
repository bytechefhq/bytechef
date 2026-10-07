/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.domain;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
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
