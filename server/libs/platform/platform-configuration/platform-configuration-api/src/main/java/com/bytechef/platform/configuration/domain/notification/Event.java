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

package com.bytechef.platform.configuration.domain.notification;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Matija Petanjek
 */
@Table("notification_event")
public class Event {

    @Id
    private Long id;

    @Column
    private Type type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Source {
        JOB, TASK
    }

    public enum Type {
        JOB_CANCELLED(Source.JOB, "CANCELLED"), JOB_CREATED(Source.JOB, "CREATED"),
        JOB_COMPLETED(Source.JOB, "COMPLETED"), JOB_FAILED(Source.JOB, "FAILED"),
        JOB_STARTED(Source.JOB, "STARTED");

        public static Type of(Source source, String value) {
            for (Type type : values()) {
                if (source == type.source && value.equals(type.value)) {
                    return type;
                }
            }

            throw new IllegalArgumentException();
        }

        Type(Source source, String value) {
            this.source = source;
            this.value = value;
        }

        private Source source;
        private String value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Event event = (Event) o;

        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
