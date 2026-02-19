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

package com.bytechef.platform.workflow.execution.repository.converter;

import com.bytechef.platform.workflow.execution.domain.TaskState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@ReadingConverter
public class StringToTaskStateValueConverter implements Converter<String, TaskState.TaskStateValue> {

    private static final List<String> ALLOWED_CLASS_PREFIXES = List.of("java.", "com.bytechef.");

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public StringToTaskStateValueConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TaskState.TaskStateValue convert(String source) {
        return source == null ? null : read(objectMapper, source);
    }

    private TaskState.TaskStateValue read(ObjectMapper objectMapper, String json) {
        try {
            TaskState.TaskStateValue taskStateValue = objectMapper.readValue(json, TaskState.TaskStateValue.class);

            String classname = taskStateValue.classname();

            if (ALLOWED_CLASS_PREFIXES.stream()
                .noneMatch(classname::startsWith)) {

                throw new IllegalArgumentException("Disallowed class in task state: " + classname);
            }

            return new TaskState.TaskStateValue(
                objectMapper.convertValue(taskStateValue.value(), Class.forName(classname)),
                classname);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
