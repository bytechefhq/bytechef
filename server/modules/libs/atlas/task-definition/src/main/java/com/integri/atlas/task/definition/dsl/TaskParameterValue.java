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

package com.integri.atlas.task.definition.dsl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public sealed interface TaskParameterValue
    extends TaskParameter
    permits
        TaskParameterValue.TaskPropertyValueBoolean,
        TaskParameterValue.TaskPropertyValueDateTime,
        TaskParameterValue.TaskPropertyValueNumber,
        TaskParameterValue.TaskPropertyValueString {
    static TaskParameterValue parameterValue(boolean value) {
        return new TaskPropertyValueBoolean(value);
    }

    static TaskParameterValue parameterValue(int value) {
        return new TaskPropertyValueNumber(value);
    }

    static TaskParameterValue parameterValue(double value) {
        return new TaskPropertyValueNumber(value);
    }

    static TaskParameterValue parameterValue(float value) {
        return new TaskPropertyValueNumber(value);
    }

    static TaskParameterValue parameterValue(LocalDateTime value) {
        return new TaskPropertyValueDateTime(value);
    }

    static TaskParameterValue parameterValue(long value) {
        return new TaskPropertyValueNumber(value);
    }

    static TaskParameterValue parameterValue(String value) {
        return new TaskPropertyValueString(value);
    }

    static List<TaskParameterValue> parameterValues(Boolean... values) {
        return Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList());
    }

    static List<TaskParameterValue> parameterValues(Double... values) {
        return Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList());
    }

    static List<TaskParameterValue> parameterValues(Integer... values) {
        return Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList());
    }

    static List<TaskParameterValue> parameterValues(Float... values) {
        return Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList());
    }

    static List<TaskParameterValue> parameterValues(LocalDateTime... values) {
        return Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList());
    }

    static List<TaskParameterValue> parameterValues(Long... values) {
        return Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList());
    }

    static List<TaskParameterValue> parameterValues(String... values) {
        return Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList());
    }

    @JsonSerialize(using = TaskPropertyValueBoolean.TaskPropertyValueTypeBooleanSerializer.class)
    final class TaskPropertyValueBoolean implements TaskParameterValue {

        private boolean value;

        private TaskPropertyValueBoolean() {}

        private TaskPropertyValueBoolean(boolean value) {
            this.value = value;
        }

        static class TaskPropertyValueTypeBooleanSerializer extends StdSerializer<TaskPropertyValueBoolean> {

            private TaskPropertyValueTypeBooleanSerializer() {
                this(null);
            }

            private TaskPropertyValueTypeBooleanSerializer(Class<TaskPropertyValueBoolean> clazz) {
                super(clazz);
            }

            @Override
            public void serialize(
                TaskPropertyValueBoolean taskPropertyValueTypeBoolean,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider
            ) throws IOException {
                jsonGenerator.writeBoolean(taskPropertyValueTypeBoolean.value);
            }
        }
    }

    @JsonSerialize(using = TaskPropertyValueDateTime.TaskPropertyValueTypeDateTimeSerializer.class)
    final class TaskPropertyValueDateTime implements TaskParameterValue {

        private LocalDateTime value;

        private TaskPropertyValueDateTime() {}

        private TaskPropertyValueDateTime(LocalDateTime value) {
            this.value = value;
        }

        static class TaskPropertyValueTypeDateTimeSerializer extends StdSerializer<TaskPropertyValueDateTime> {

            private TaskPropertyValueTypeDateTimeSerializer() {
                this(null);
            }

            private TaskPropertyValueTypeDateTimeSerializer(Class<TaskPropertyValueDateTime> clazz) {
                super(clazz);
            }

            @Override
            public void serialize(
                TaskPropertyValueDateTime taskPropertyValueDateTime,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider
            ) throws IOException {
                if (taskPropertyValueDateTime.value == null) {
                    jsonGenerator.writeString("");
                } else {
                    jsonGenerator.writeString(taskPropertyValueDateTime.value.toString());
                }
            }
        }
    }

    @JsonSerialize(using = TaskPropertyValueNumber.TaskPropertyValueTypeNumberSerializer.class)
    final class TaskPropertyValueNumber implements TaskParameterValue {

        private Number value;

        private TaskPropertyValueNumber() {}

        private TaskPropertyValueNumber(Number value) {
            this.value = value;
        }

        static class TaskPropertyValueTypeNumberSerializer extends StdSerializer<TaskPropertyValueNumber> {

            private TaskPropertyValueTypeNumberSerializer() {
                this(null);
            }

            private TaskPropertyValueTypeNumberSerializer(Class<TaskPropertyValueNumber> clazz) {
                super(clazz);
            }

            @Override
            public void serialize(
                TaskPropertyValueNumber taskPropertyValueNumber,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider
            ) throws IOException {
                if (taskPropertyValueNumber.value instanceof Integer) {
                    jsonGenerator.writeNumber((Integer) taskPropertyValueNumber.value);
                } else if (taskPropertyValueNumber.value instanceof Long) {
                    jsonGenerator.writeNumber((Long) taskPropertyValueNumber.value);
                } else if (taskPropertyValueNumber.value instanceof Float) {
                    jsonGenerator.writeNumber((Float) taskPropertyValueNumber.value);
                } else {
                    jsonGenerator.writeNumber((Double) taskPropertyValueNumber.value);
                }
            }
        }
    }

    @JsonSerialize(using = TaskPropertyValueString.TaskPropertyValueTypeStringSerializer.class)
    final class TaskPropertyValueString implements TaskParameterValue {

        private String value;

        private TaskPropertyValueString() {}

        private TaskPropertyValueString(String value) {
            this.value = value;
        }

        static class TaskPropertyValueTypeStringSerializer extends StdSerializer<TaskPropertyValueString> {

            private TaskPropertyValueTypeStringSerializer() {
                this(null);
            }

            private TaskPropertyValueTypeStringSerializer(Class<TaskPropertyValueString> clazz) {
                super(clazz);
            }

            @Override
            public void serialize(
                TaskPropertyValueString taskPropertyValueString,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider
            ) throws IOException {
                jsonGenerator.writeString(taskPropertyValueString.value);
            }
        }
    }
}
