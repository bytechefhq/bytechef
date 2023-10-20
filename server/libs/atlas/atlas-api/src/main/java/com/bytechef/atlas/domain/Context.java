
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 */
@Table
public final class Context implements Persistable<Long> {

    public enum Classname {
        JOB(1), TASK_EXECUTION(2);

        private final int id;

        Classname(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Classname valueOf(int id) {
            return switch (id) {
                case 1 -> Classname.JOB;
                case 2 -> Classname.TASK_EXECUTION;
                default -> throw new IllegalStateException("Unexpected value: " + id);
            };
        }
    }

    @Column
    private Integer classnameId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Id
    private Long id;

    @Transient
    private boolean isNew;

    @Column("sub_stack_id")
    private Integer subStackId;

    @Column("stack_id")
    private Long stackId;

    @Column("value")
    private MapWrapper value;

    public Context() {
        this.value = new MapWrapper();
    }

    public Context(Map<String, Object> value) {
        Assert.notNull(value, "'value' must not be null.");

        this.value = new MapWrapper(value);
    }

    public Context(long stackId, Classname classname) {
        this(stackId, null, classname, new HashMap<>());
    }

    public Context(long stackId, Classname classname, Map<String, Object> value) {
        this(stackId, null, classname, value);
    }

    public Context(long stackId, int subStackId, Classname classname) {
        this(stackId, subStackId, classname, new HashMap<>());
    }

    public Context(long stackId, Integer subStackId, Classname classname, Map<String, Object> value) {
        Assert.notNull(classname, "'classname' must not be null.");
        Assert.notNull(value, "'value' must not be null.");

        this.stackId = stackId;
        this.subStackId = subStackId;
        this.classnameId = classname.getId();
        this.value = new MapWrapper(value);
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

    public Integer getClassnameId() {
        return classnameId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public Integer getSubStackId() {
        return subStackId;
    }

    public Long getStackId() {
        return stackId;
    }

    public Map<String, Object> getValue() {
        return Collections.unmodifiableMap(value.getMap());
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public String toString() {
        return "Context{" +
            "classnameId=" + classnameId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", id=" + id +
            ", isNew=" + isNew +
            ", subStackId=" + subStackId +
            ", stackId=" + stackId +
            ", value=" + value +
            '}';
    }
}
