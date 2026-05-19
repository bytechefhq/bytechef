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

package com.bytechef.platform.configuration.domain;

import com.bytechef.commons.data.jdbc.wrapper.EncryptedMapWrapper;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.credential.store.CredentialSecret;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
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
 * @author Ivica Cardic
 */
@Table("property")
public class Property implements CredentialSecret {

    public enum Scope {
        PLATFORM, AUTOMATION, EMBEDDED, WORKSPACE, PROJECT, INTEGRATION
    }

    @Id
    private Long id;

    @Column("credential_ref")
    private @Nullable String credentialRef;

    @Column("credential_store_type")
    private int credentialStoreType;

    @Column
    private String key;

    @Column
    private int scope;

    @Column
    private Long scopeId;

    @Column
    private EncryptedMapWrapper value;

    @Column
    private @Nullable Integer environment;

    @Column
    private boolean enabled;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Property that = (Property) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Object get(String key) {
        Map<String, ?> map = value.getMap();

        return map.get(key);
    }

    @Override
    public @Nullable String getCredentialRef() {
        return credentialRef;
    }

    @Override
    public CredentialStoreType getCredentialStoreType() {
        return CredentialStoreType.values()[credentialStoreType];
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    @Override
    public Map<String, ?> getPayload() {
        return getValue();
    }

    public Scope getScope() {
        return Scope.values()[scope];
    }

    public Long getScopeId() {
        return scopeId;
    }

    public Map<String, ?> getValue() {
        return Collections.unmodifiableMap(value.getMap());
    }

    public @Nullable Integer getEnvironment() {
        return environment;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public void setCredentialRef(@Nullable String credentialRef) {
        this.credentialRef = credentialRef;
    }

    @Override
    public void setCredentialStoreType(CredentialStoreType credentialStoreType) {
        this.credentialStoreType = credentialStoreType.ordinal();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void setPayload(Map<String, ?> payload) {
        setValue(payload);
    }

    public void setScope(Scope scope) {
        this.scope = scope.ordinal();
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public void setValue(Map<String, ?> value) {
        if (!MapUtils.isEmpty(value)) {
            this.value = new EncryptedMapWrapper(value);
        }
    }

    public void setEnvironment(@Nullable Integer environment) {
        this.environment = environment;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Properties{" +
            "id=" + id +
            ", key='" + key + '\'' +
            ", scope=" + scope +
            ", scopeId=" + scopeId +
            ", environment=" + environment +
            ", credentialStoreType=" + credentialStoreType +
            ", credentialRef='" + credentialRef + '\'' +
            ", value=" + value +
            ", enabled=" + enabled +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
