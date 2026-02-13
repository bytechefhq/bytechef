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

import com.bytechef.commons.util.CollectionUtils;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
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
@Table
public class User {

    @Column
    private boolean activated;

    @Column("activation_key")
    private String activationKey;

    @Column("auth_provider")
    private String authProvider = "LOCAL";

    @MappedCollection(idColumn = "user_id")
    private Set<UserAuthority> authorities = new HashSet<>();

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private String email;

    @Column("first_name")
    private String firstName;

    @Id
    private Long id;

    @Column("image_url")
    private String imageUrl;

    @Column("lang_key")
    private String langKey;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("last_name")
    private String lastName;

    @Column
    private String login;

    @Column("password_hash")
    private String password;

    @Column("provider_id")
    private String providerId;

    @Column("reset_date")
    private Instant resetDate = null;

    @Column("reset_key")
    private String resetKey;

    @Column("totp_enabled")
    private boolean totpEnabled;

    @Column("totp_secret")
    private String totpSecret;

    @Column("uuid")
    private UUID uuid;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof User)) {
            return false;
        }

        return id != null && id.equals(((User) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getActivationKey() {
        return activationKey;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public List<Long> getAuthorityIds() {
        return authorities
            .stream()
            .map(UserAuthority::getAuthorityId)
            .toList();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getEmail() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLangKey() {
        return langKey;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassword() {
        return password;
    }

    public String getProviderId() {
        return providerId;
    }

    public Instant getResetDate() {
        return resetDate;
    }

    public String getResetKey() {
        return resetKey;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUuidAsString() {
        return uuid == null ? null : uuid.toString();
    }

    public boolean isActivated() {
        return activated;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setAuthorityIds(List<Long> authorityIds) {
        this.authorities = new HashSet<>();

        if (!CollectionUtils.isEmpty(authorityIds)) {
            for (Long authorityId : authorityIds) {
                authorities.add(new UserAuthority(authorityId));
            }
        }
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public void setAuthorities(Set<Authority> authorities) {
        if (!CollectionUtils.isEmpty(authorities)) {
            setAuthorityIds(CollectionUtils.map(authorities, Authority::getId));
        }
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setLogin(String login) {
        this.login = StringUtils.lowerCase(login, Locale.ENGLISH);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public void setResetKey(String resetKey) {
        this.resetKey = resetKey;
    }

    public void setResetDate(Instant resetDate) {
        this.resetDate = resetDate;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public void setUuid(String uuid) {
        if (uuid == null) {
            this.uuid = null;
        } else {
            this.uuid = UUID.fromString(uuid);
        }
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "User{" +
            "id=" + id +
            ", login='" + login + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", email='" + email + '\'' +
            ", imageUrl='" + imageUrl + '\'' +
            ", activated='" + activated + '\'' +
            ", langKey='" + langKey + '\'' +
            ", activationKey='" + activationKey + '\'' +
            ", authProvider='" + authProvider + '\'' +
            "}";
    }
}
