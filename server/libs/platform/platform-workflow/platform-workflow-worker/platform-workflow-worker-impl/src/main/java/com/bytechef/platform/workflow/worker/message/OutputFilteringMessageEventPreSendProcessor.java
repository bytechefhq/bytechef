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

package com.bytechef.platform.workflow.worker.message;

import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.event.MessageEventPreSendProcessor;
import com.bytechef.platform.component.constant.MetadataConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Filters task execution output to include only the property paths referenced by downstream tasks. The referenced paths
 * are provided as metadata on the task execution by
 * {@link com.bytechef.platform.workflow.coordinator.task.dispatcher.OutputReferenceTaskDispatcherPreSendProcessor}.
 *
 * @author Ivica Cardic
 */
@Component
class OutputFilteringMessageEventPreSendProcessor implements MessageEventPreSendProcessor {

    private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("^(.+?)\\[(\\d+)]$");
    private static final Logger logger = LoggerFactory.getLogger(OutputFilteringMessageEventPreSendProcessor.class);

    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    OutputFilteringMessageEventPreSendProcessor(TaskFileStorage taskFileStorage) {
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public MessageEvent<?> process(MessageEvent<?> messageEvent) {
        if (!(messageEvent instanceof TaskExecutionCompleteEvent taskExecutionCompleteEvent)) {
            return messageEvent;
        }

        TaskExecution taskExecution = taskExecutionCompleteEvent.getTaskExecution();

        FileEntry output = taskExecution.getOutput();

        if (output == null) {
            return messageEvent;
        }

        Map<String, ?> metadata = taskExecution.getMetadata();

        Object referencePaths = metadata.get(MetadataConstants.OUTPUT_REFERENCE_PATHS);

        if (!(referencePaths instanceof Collection<?> referencePathCollection) || referencePathCollection.isEmpty()) {
            return messageEvent;
        }

        Set<String> paths = Set.copyOf(
            referencePathCollection.stream()
                .map(Object::toString)
                .toList());

        Object outputValue = taskFileStorage.readTaskExecutionOutput(output);

        if (!(outputValue instanceof Map<?, ?> outputMap)) {
            return messageEvent;
        }

        Map<String, Object> filteredOutput = filterOutputByPaths(outputMap, paths);

        FileEntry filteredOutputFileEntry = taskFileStorage.storeTaskExecutionOutput(
            Objects.requireNonNull(taskExecution.getJobId()),
            Objects.requireNonNull(taskExecution.getId()), filteredOutput);

        taskExecution.setOutput(filteredOutputFileEntry);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Filtered output for task name='{}', type='{}': kept {} paths out of {} top-level keys",
                taskExecution.getName(), taskExecution.getType(), paths.size(), outputMap.size());
        }

        return messageEvent;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> filterOutputByPaths(Map<?, ?> outputMap, Set<String> paths) {
        Map<String, Object> filteredOutput = new HashMap<>();

        for (String path : paths) {
            String[] segments = path.split("\\.");

            Object currentValue = outputMap;

            for (String segment : segments) {
                currentValue = resolveSegment(currentValue, segment);

                if (currentValue == null) {
                    break;
                }
            }

            if (currentValue != null) {
                setNestedValue(filteredOutput, segments, currentValue);
            }
        }

        return filteredOutput;
    }

    private static Object resolveSegment(Object currentValue, String segment) {
        Matcher arrayMatcher = ARRAY_INDEX_PATTERN.matcher(segment);

        if (arrayMatcher.matches()) {
            String key = arrayMatcher.group(1);
            int index = Integer.parseInt(arrayMatcher.group(2));

            if (currentValue instanceof Map<?, ?> currentMap) {
                Object listValue = currentMap.get(key);

                if (listValue instanceof List<?> list && index < list.size()) {
                    return list.get(index);
                }

                return null;
            }

            return null;
        }

        if (currentValue instanceof Map<?, ?> currentMap) {
            return currentMap.get(segment);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static void setNestedValue(Map<String, Object> target, String[] segments, Object value) {
        Map<String, Object> currentMap = target;

        for (int i = 0; i < segments.length - 1; i++) {
            currentMap = (Map<String, Object>) currentMap.computeIfAbsent(segments[i], key -> new HashMap<>());
        }

        currentMap.put(segments[segments.length - 1], value);
    }
}
