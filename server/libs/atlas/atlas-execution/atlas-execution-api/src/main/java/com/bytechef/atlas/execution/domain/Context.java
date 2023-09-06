
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

package com.bytechef.atlas.execution.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import com.bytechef.file.storage.domain.FileEntry;
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
                default -> throw new IllegalStateException("Unexpected value: %s".formatted(id));
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

    @Column("sub_stack_id")
    private Integer subStackId;

    @Column("stack_id")
    private Long stackId;

    @Column("value")
    private FileEntry value;

    public Context() {
    }

    public Context(FileEntry value) {
        Assert.notNull(value, "'value' must not be null");

        this.value = value;
    }

    public Context(long stackId, Classname classname) {
        this(stackId, null, classname, null);
    }

    public Context(long stackId, Classname classname, FileEntry value) {
        this(stackId, null, classname, value);
    }

    public Context(long stackId, int subStackId, Classname classname) {
        this(stackId, subStackId, classname, null);
    }

    public Context(long stackId, Integer subStackId, Classname classname, FileEntry value) {
        Assert.notNull(classname, "'classname' must not be null");
        Assert.notNull(value, "'value' must not be null");

        this.stackId = stackId;
        this.subStackId = subStackId;
        this.classnameId = classname.getId();
        this.value = value;
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

    public Classname getClassname() {
        return Classname.valueOf(classnameId);
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

    public FileEntry getValue() {
        return value;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Context{" +
            "id=" + id +
            ", classnameId=" + classnameId +
            ", subStackId=" + subStackId +
            ", stackId=" + stackId +
            ", value=" + value +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            '}';
    }
}
