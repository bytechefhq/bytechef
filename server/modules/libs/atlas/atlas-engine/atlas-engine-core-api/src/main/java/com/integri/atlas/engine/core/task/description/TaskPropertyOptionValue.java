/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.core.task.description;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

/**
 * @author Ivica Cardic
 */
public interface TaskPropertyOptionValue {
    static TaskPropertyOptionValue optionValue(int value) {
        return new TaskPropertyOptionValueNumber(value);
    }

    static TaskPropertyOptionValue optionValue(String value) {
        return new TaskPropertyOptionValueString(value);
    }

    @JsonSerialize(using = TaskPropertyOptionValueNumber.TaskPropertyValueTypeNumberSerializer.class)
    final class TaskPropertyOptionValueNumber implements TaskPropertyOptionValue {

        private int value;

        private TaskPropertyOptionValueNumber() {}

        private TaskPropertyOptionValueNumber(int value) {
            this.value = value;
        }

        static class TaskPropertyValueTypeNumberSerializer extends StdSerializer<TaskPropertyOptionValueNumber> {

            private TaskPropertyValueTypeNumberSerializer() {
                this(null);
            }

            private TaskPropertyValueTypeNumberSerializer(Class<TaskPropertyOptionValueNumber> clazz) {
                super(clazz);
            }

            @Override
            public void serialize(
                TaskPropertyOptionValueNumber taskPropertyOptionValueNumber,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider
            ) throws IOException {
                jsonGenerator.writeNumber(taskPropertyOptionValueNumber.value);
            }
        }
    }

    @JsonSerialize(using = TaskPropertyOptionValueString.TaskPropertyValueTypeStringSerializer.class)
    final class TaskPropertyOptionValueString implements TaskPropertyOptionValue {

        private String value;

        private TaskPropertyOptionValueString() {}

        private TaskPropertyOptionValueString(String value) {
            this.value = value;
        }

        static class TaskPropertyValueTypeStringSerializer extends StdSerializer<TaskPropertyOptionValueString> {

            private TaskPropertyValueTypeStringSerializer() {
                this(null);
            }

            private TaskPropertyValueTypeStringSerializer(Class<TaskPropertyOptionValueString> clazz) {
                super(clazz);
            }

            @Override
            public void serialize(
                TaskPropertyOptionValueString taskPropertyOptionValueString,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider
            ) throws IOException {
                jsonGenerator.writeString(taskPropertyOptionValueString.value);
            }
        }
    }
}
