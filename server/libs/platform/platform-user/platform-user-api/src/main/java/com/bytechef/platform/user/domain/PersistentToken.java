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

package com.bytechef.platform.user.domain;

import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("persistent_token")
public class PersistentToken implements Persistable<String> {

    private static final int MAX_USER_AGENT_LEN = 255;

    @Transient
    private boolean isNew;

    @Id
    private String series;

    @Column("token_value")
    private String tokenValue;

    @Column("token_date")
    private LocalDate tokenDate;

    @Column("ip_address")
    private String ipAddress;

    @Column("user_agent")
    private String userAgent;

    private AggregateReference<User, Long> userId;

    @Override
    public String getId() {
        return series;
    }

    public String getSeries() {
        return series;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public LocalDate getTokenDate() {
        return tokenDate;
    }

    public void setTokenDate(LocalDate tokenDate) {
        this.tokenDate = tokenDate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Long getUserId() {
        return userId.getId();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public void setUserAgent(String userAgent) {
        if (userAgent.length() >= MAX_USER_AGENT_LEN) {
            this.userAgent = userAgent.substring(0, MAX_USER_AGENT_LEN - 1);
        } else {
            this.userAgent = userAgent;
        }
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public void setUser(User user) {
        this.userId = AggregateReference.to(user.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistentToken)) {
            return false;
        }
        return Objects.equals(series, ((PersistentToken) o).series);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(series);
    }

    @Override
    public String toString() {
        return "PersistentToken{" +
            "series='" + series + '\'' +
            ", tokenValue='" + tokenValue + '\'' +
            ", tokenDate=" + tokenDate +
            ", ipAddress='" + ipAddress + '\'' +
            ", userAgent='" + userAgent + '\'' +
            "}";
    }
}
