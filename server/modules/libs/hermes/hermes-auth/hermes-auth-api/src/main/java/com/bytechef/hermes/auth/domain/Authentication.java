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

package com.bytechef.hermes.auth.domain;

import com.bytechef.atlas.Accessor;
import com.bytechef.atlas.MapObject;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class Authentication {

    private String id;
    private String name;
    private String type;
    private Map<String, Object> properties;
    private Date createTime;
    private Date updateTime;

    /**
     * Return the ID of the task auth.
     */
    public String getId() {
        return id;
    }

    /** Return the name of the task auth. */
    public String getName() {
        return name;
    }

    /**
     * Return the type of the task auth.
     */
    public String getType() {
        return type;
    }

    /**
     * Return the task auth properties.
     */
    public Accessor getProperties() {
        return properties != null ? new MapObject(properties) : new MapObject();
    }

    /**
     * Return the task auth property value.
     */
    public <T> T getProperty(String name) {
        Accessor properties = getProperties();

        return properties.get(name);
    }

    /**
     * Return the task auth property value.
     */
    public <T> T getProperty(String name, T defaultValue) {
        Accessor properties = getProperties();

        T value = properties.get(name);

        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * Return the time when the task auth was originally created.
     *
     * @return {@link Date}
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * Return the time when the task auth was updated.
     *
     * @return {@link Date}
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
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

        Authentication that = (Authentication) o;

        return id.equals(that.id)
                && name.equals(that.name)
                && type.equals(that.type)
                && properties.equals(that.properties)
                && createTime.equals(that.createTime)
                && updateTime.equals(that.updateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
