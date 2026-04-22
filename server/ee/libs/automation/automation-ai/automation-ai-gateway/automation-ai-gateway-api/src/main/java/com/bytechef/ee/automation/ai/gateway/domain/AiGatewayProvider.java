/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import com.bytechef.commons.data.jdbc.wrapper.EncryptedStringWrapper;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_provider")
public class AiGatewayProvider {

    @Column("api_key")
    private EncryptedStringWrapper apiKey;

    @Column("base_url")
    private String baseUrl;

    @Column
    private String config;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column
    private int type;

    @Version
    private int version;

    private AiGatewayProvider() {
    }

    public AiGatewayProvider(String name, AiGatewayProviderType type, String apiKey) {
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(type, "type must not be null");
        Validate.notBlank(apiKey, "apiKey must not be blank");

        this.apiKey = new EncryptedStringWrapper(apiKey);
        this.enabled = true;
        this.name = name;
        this.type = type.ordinal();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayProvider aiGatewayProvider)) {
            return false;
        }

        return Objects.equals(id, aiGatewayProvider.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * Returns the decrypted API key wrapped in {@link ApiKey}. Prefer this over {@link #revealApiKey()} — the wrapper
     * forces callers to be explicit when they want plaintext (via {@link ApiKey#reveal()}) and keeps the plaintext out
     * of {@code toString()} and serialization.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Nullable
    public ApiKey getApiKeyWrapped() {
        return apiKey == null ? null : ApiKey.ofNullable(apiKey.getValue());
    }

    /**
     * Returns the decrypted API key as raw plaintext with an explicit, auditable method name. Every call to this method
     * is a plaintext-leak surface — grep the codebase for {@code revealApiKey} to enumerate them.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String revealApiKey() {
        return apiKey == null ? null : apiKey.getValue();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getConfig() {
        return config;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
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

    public AiGatewayProviderType getType() {
        return AiGatewayProviderType.fromOrdinal(type);
    }

    public int getVersion() {
        return version;
    }

    /**
     * @deprecated Use {@link #setApiKey(ApiKey)} to keep the {@code ApiKey} type-safe wrapper end-to-end. The raw
     *             {@code String} overload exists only so in-flight callers that still construct providers from REST
     *             request bodies compile; new callers should wrap the value in {@link ApiKey#of(String)} at the
     *             boundary. Planned removal once all callers migrate.
     */
    @Deprecated(forRemoval = true, since = "0.26")
    public void setApiKey(String apiKey) {
        Validate.notBlank(apiKey, "apiKey must not be blank");

        this.apiKey = new EncryptedStringWrapper(apiKey);
    }

    public void setApiKey(ApiKey apiKey) {
        Validate.notNull(apiKey, "apiKey must not be null");

        this.apiKey = new EncryptedStringWrapper(apiKey.reveal());
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(baseUrl);

                if (uri.getScheme() == null || !uri.getScheme()
                    .matches("https?")) {
                    throw new IllegalArgumentException(
                        "baseUrl must use http or https scheme, got: " + uri.getScheme());
                }
            } catch (IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            } catch (Exception exception) {
                throw new IllegalArgumentException(
                    "baseUrl is not a valid URL: " + baseUrl, exception);
            }
        }

        this.baseUrl = baseUrl;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    @Override
    public String toString() {
        return "AiGatewayProvider{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", type=" + getType() +
            ", enabled=" + enabled +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
