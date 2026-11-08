/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
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

    @Column("mcp_embedded")
    private boolean mcpEmbedded;

    @Column("mcp_automation")
    private boolean mcpAutomation;

    @Column("mcp_management")
    private boolean mcpManagement;

    @MappedCollection(idColumn = "identity_provider_id")
    private Set<IdentityProviderAuthorityMapping> authorityMappings = new HashSet<>();

    @Column("authorities_claim")
    private String authoritiesClaim;

    @Column("metadata_uri")
    private String metadataUri;

    @Column("validate_mcp_audience")
    private boolean validateMcpAudience;

    @Column("mfa_method")
    private String mfaMethod;

    @Column("mfa_required")
    private boolean mfaRequired;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column("name_id_format")
    private String nameIdFormat;

    @Column("scim_api_key")
    private String scimApiKey;

    @Column
    private String scopes = "openid,profile,email";

    @Column("signing_certificate")
    private String signingCertificate;

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

    public boolean isMcpEmbedded() {
        return mcpEmbedded;
    }

    public boolean isMcpAutomation() {
        return mcpAutomation;
    }

    public boolean isMcpManagement() {
        return mcpManagement;
    }

    public Map<String, String> getAuthorityMappings() {
        return authorityMappings.stream()
            .collect(
                Collectors.toMap(
                    IdentityProviderAuthorityMapping::getExternalGroup,
                    IdentityProviderAuthorityMapping::getAuthority));
    }

    public String getAuthoritiesClaim() {
        return authoritiesClaim;
    }

    public boolean isValidateMcpAudience() {
        return validateMcpAudience;
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

    public String getMetadataUri() {
        return metadataUri;
    }

    public String getMfaMethod() {
        return mfaMethod;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public String getName() {
        return name;
    }

    public String getNameIdFormat() {
        return nameIdFormat;
    }

    public String getScimApiKey() {
        return scimApiKey;
    }

    public String getScopes() {
        return scopes;
    }

    public String getSigningCertificate() {
        return signingCertificate;
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

    public void setMcpEmbedded(boolean mcpEmbedded) {
        this.mcpEmbedded = mcpEmbedded;
    }

    public void setMcpAutomation(boolean mcpAutomation) {
        this.mcpAutomation = mcpAutomation;
    }

    public void setMcpManagement(boolean mcpManagement) {
        this.mcpManagement = mcpManagement;
    }

    public void setAuthorityMappings(Map<String, String> authorityMappings) {
        this.authorityMappings = new HashSet<>();

        if (authorityMappings != null) {
            authorityMappings.forEach(
                (externalGroup, authority) -> this.authorityMappings.add(
                    new IdentityProviderAuthorityMapping(externalGroup, authority)));
        }
    }

    public void setAuthoritiesClaim(String authoritiesClaim) {
        this.authoritiesClaim = authoritiesClaim;
    }

    public void setValidateMcpAudience(boolean validateMcpAudience) {
        this.validateMcpAudience = validateMcpAudience;
    }

    public void setMetadataUri(String metadataUri) {
        this.metadataUri = metadataUri;
    }

    public void setMfaMethod(String mfaMethod) {
        this.mfaMethod = mfaMethod;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNameIdFormat(String nameIdFormat) {
        this.nameIdFormat = nameIdFormat;
    }

    public void setScimApiKey(String scimApiKey) {
        this.scimApiKey = scimApiKey;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public void setSigningCertificate(String signingCertificate) {
        this.signingCertificate = signingCertificate;
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
