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

package com.bytechef.platform.notification.domain;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Matija Petanjek
 */
@Table("notification_event")
public class NotificationEvent {

    public enum Source {
        JOB, TASK
    }

    public enum Type {

        JOB_CANCELLED(Source.JOB, "CANCELLED"), JOB_CREATED(Source.JOB, "CREATED"),
        JOB_COMPLETED(Source.JOB, "COMPLETED"), JOB_FAILED(Source.JOB, "FAILED"),
        JOB_STARTED(Source.JOB, "STARTED"), JOB_STOPPED(Source.JOB, "STOPPED");

        private final Source source;
        private final String value;

        Type(Source source, String value) {
            this.source = source;
            this.value = value;
        }

        public static Type of(Source source, String value) {
            for (Type type : values()) {
                if (source == type.source && value.equals(type.value)) {
                    return type;
                }
            }

            throw new IllegalArgumentException(String.format("Invalid source %s and value %s", source, value));
        }
    }

    @Id
    private Long id;

    @Column
    private int type;

    public Long getId() {
        return id;
    }

    public Type getType() {
        return Type.values()[type];
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(Type type) {
        this.type = type.ordinal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NotificationEvent notificationEvent = (NotificationEvent) o;

        return Objects.equals(id, notificationEvent.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "NotificationEvent{" +
            "id=" + id +
            ", type=" + type +
            '}';
    }
}
