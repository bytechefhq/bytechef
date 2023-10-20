/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 */
@Table
public final class Context implements Persistable<String> {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Id
    private String id;

    @Column("stack_id")
    private String stackId;

    @Column("value")
    private MapWrapper value;

    public Context() {
        value = new MapWrapper();
    }

    public Context(Map<String, Object> value) {
        Assert.notNull(value, "id cannot be value");

        this.value = new MapWrapper(value);
    }

    public Context(String key, Object value) {
        this(Collections.singletonMap(key, value));
    }

    public Context(Context context) {
        Assert.notNull(context, "Context cannot be null");

        this.id = context.getId();
        this.stackId = context.getStackId();
        this.value = new MapWrapper(context.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Context context = (Context) o;

        return Objects.equals(id, context.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getId() {
        return id;
    }

    public String getStackId() {
        return stackId;
    }

    public Map<String, Object> getValue() {
        return Collections.unmodifiableMap(value.getMap());
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void put(String key, Object value) {
        this.value.put(key, value);
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setStackId(String stackId) {
        this.stackId = stackId;
    }

    public void setValue(Map<String, Object> value) {
        this.value = new MapWrapper(value);
    }

    @Override
    public String toString() {
        return "Context{" + "createdBy='"
                + createdBy + '\'' + ", createdDate="
                + createdDate + ", id='"
                + id + '\'' + ", stackId='"
                + stackId + '\'' + ", value="
                + value + '}';
    }
}
