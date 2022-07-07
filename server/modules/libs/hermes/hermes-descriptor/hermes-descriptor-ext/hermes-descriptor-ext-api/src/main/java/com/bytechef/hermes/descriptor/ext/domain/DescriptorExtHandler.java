/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.descriptor.ext.domain;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public class DescriptorExtHandler {
    private String name;
    private Set<Double> versions = new HashSet<>();
    private boolean authenticationExists;
    private String type;
    private Map<String, Object> properties = new HashMap<>();
    private Date createTime;
    private Date updateTime;

    public DescriptorExtHandler() {}

    public DescriptorExtHandler(
            String name, Double version, boolean authenticationExists, String type, Map<String, Object> properties) {
        this(name, Set.of(version), authenticationExists, type, properties);
    }

    public DescriptorExtHandler(
            String name,
            Set<Double> versions,
            boolean authenticationExists,
            String type,
            Map<String, Object> properties) {
        this.name = name;
        this.versions = new HashSet<>(versions);
        this.authenticationExists = authenticationExists;
        this.type = type;
        this.properties = properties;
    }

    public void addVersion(Double version) {
        versions.add(version);
    }

    public String getName() {
        return name;
    }

    public Set<Double> getVersions() {
        return versions;
    }

    public boolean isAuthenticationExists() {
        return authenticationExists;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public <T> T getProperty(String key) {
        return (T) properties.get(key);
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersions(Set<Double> versions) {
        this.versions = versions;
    }

    public void setAuthenticationExists(boolean authenticationExists) {
        this.authenticationExists = authenticationExists;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DescriptorExtHandler that = (DescriptorExtHandler) o;

        return authenticationExists == that.authenticationExists
                && name.equals(that.name)
                && versions.equals(that.versions)
                && type.equals(that.type)
                && properties.equals(that.properties)
                && createTime.equals(that.createTime)
                && updateTime.equals(that.updateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
