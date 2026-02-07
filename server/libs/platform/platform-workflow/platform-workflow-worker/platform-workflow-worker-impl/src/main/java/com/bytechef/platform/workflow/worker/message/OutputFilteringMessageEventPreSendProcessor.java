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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
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

    private static Map<String, Object> filterOutputByPaths(Map<?, ?> outputMap, Set<String> paths) {
        Set<String> normalizedPaths = normalizePaths(paths);

        Map<String, Object> filteredOutput = new HashMap<>();

        for (String path : normalizedPaths) {
            String[] segments = path.split("\\.");

            Object resolvedValue = outputMap;

            for (String segment : segments) {
                resolvedValue = resolveSegment(resolvedValue, segment);

                if (resolvedValue == null) {
                    break;
                }
            }

            if (resolvedValue != null) {
                setNestedValue(filteredOutput, segments, resolvedValue, outputMap);
            }
        }

        return filteredOutput;
    }

    private static Set<String> normalizePaths(Set<String> paths) {
        TreeSet<String> sortedPaths = new TreeSet<>(
            Comparator.<String>comparingInt(path -> path.split("\\.").length)
                .thenComparing(Comparator.naturalOrder()));

        sortedPaths.addAll(paths);

        Set<String> normalizedPaths = new TreeSet<>(
            Comparator.<String>comparingInt(path -> path.split("\\.").length)
                .thenComparing(Comparator.naturalOrder()));

        for (String path : sortedPaths) {
            boolean coveredByParent = false;

            for (String includedPath : normalizedPaths) {
                if (path.startsWith(includedPath + ".") || path.equals(includedPath)) {
                    coveredByParent = true;

                    break;
                }
            }

            if (!coveredByParent) {
                normalizedPaths.add(path);
            }
        }

        return normalizedPaths;
    }

    private static Object resolveSegment(Object currentValue, String segment) {
        if (segment == null) {
            return null;
        }

        int firstBracket = segment.indexOf('[');

        if (firstBracket == -1) {
            if (currentValue instanceof Map<?, ?> currentMap) {
                return currentMap.get(segment);
            }

            return null;
        }

        int length = segment.length();

        if (firstBracket == 0) {
            return resolveArrayIndexes(currentValue, segment, 0, length);
        }

        String key = segment.substring(0, firstBracket);
        Object value;

        if (currentValue instanceof Map<?, ?> currentMap) {
            value = currentMap.get(key);
        } else {
            return null;
        }

        return resolveArrayIndexes(value, segment, firstBracket, length);
    }

    private static Object resolveArrayIndexes(Object value, String segment, int startPosition, int length) {
        int position = startPosition;

        while (position < length && segment.charAt(position) == '[') {
            int closing = segment.indexOf(']', position);

            if (closing == -1) {
                return null;
            }

            int index = extractIndexFromBracket(segment, position, closing);

            if (index < 0) {
                return null;
            }

            if (!(value instanceof List<?> list) || index >= list.size()) {
                return null;
            }

            value = list.get(index);
            position = closing + 1;
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    private static void setNestedValue(
        Map<String, Object> target, String[] segments, Object value, Map<?, ?> originalOutputMap) {

        Map<String, Object> currentFilteredMap = target;
        Object currentOriginal = originalOutputMap;

        for (int segmentIndex = 0; segmentIndex < segments.length - 1; segmentIndex++) {
            String segment = segments[segmentIndex];
            int firstBracket = segment.indexOf('[');

            if (firstBracket != -1) {
                String key = segment.substring(0, firstBracket);

                Object originalListValue = resolveMapKey(currentOriginal, key);

                if (!(originalListValue instanceof List<?> originalList)) {
                    return;
                }

                Object existingInFiltered = currentFilteredMap.get(key);

                if (!(existingInFiltered instanceof List<?>)) {
                    List<Object> mutableList = createNullFilledList(originalList.size());

                    currentFilteredMap.put(key, mutableList);

                    existingInFiltered = mutableList;
                }

                List<Object> filteredList = (List<Object>) existingInFiltered;
                ListNavigationResult navigationResult = navigateToDeepestList(
                    originalList, segment, firstBracket, filteredList);

                if (navigationResult == null) {
                    return;
                }

                Object elementAtIndex = navigationResult.filteredList.get(navigationResult.index);

                if (!(elementAtIndex instanceof Map)) {
                    Map<String, Object> newMap = new HashMap<>();

                    navigationResult.filteredList.set(navigationResult.index, newMap);

                    elementAtIndex = newMap;
                }

                currentFilteredMap = (Map<String, Object>) elementAtIndex;
                currentOriginal = navigationResult.originalElement;
            } else {
                currentOriginal = resolveMapKey(currentOriginal, segment);

                currentFilteredMap =
                    (Map<String, Object>) currentFilteredMap.computeIfAbsent(segment, unusedKey -> new HashMap<>());
            }
        }

        String lastSegment = segments[segments.length - 1];
        int firstBracket = lastSegment.indexOf('[');

        if (firstBracket != -1) {
            String key = lastSegment.substring(0, firstBracket);

            Object originalListValue = resolveMapKey(currentOriginal, key);

            if (!(originalListValue instanceof List<?> originalList)) {
                return;
            }

            Object existingInFiltered = currentFilteredMap.get(key);

            if (!(existingInFiltered instanceof List<?>)) {
                List<Object> mutableList = createNullFilledList(originalList.size());

                currentFilteredMap.put(key, mutableList);

                existingInFiltered = mutableList;
            }

            setValueInList((List<Object>) existingInFiltered, lastSegment, firstBracket, value, originalList);
        } else {
            currentFilteredMap.put(lastSegment, value);
        }
    }

    private static Object resolveMapKey(Object source, String key) {
        if (source instanceof Map<?, ?> map) {
            return map.get(key);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static ListNavigationResult navigateToDeepestList(
        List<?> originalList, String segment, int firstBracket, List<Object> filteredList) {

        int position = firstBracket;
        int length = segment.length();
        Object currentOriginal = originalList;
        List<Object> currentFiltered = filteredList;

        while (position < length && segment.charAt(position) == '[') {
            int closing = segment.indexOf(']', position);

            if (closing == -1) {
                return null;
            }

            int index = extractIndexFromBracket(segment, position, closing);

            if (index < 0) {
                return null;
            }

            if (!(currentOriginal instanceof List<?> list) || index >= list.size()) {
                return null;
            }

            Object originalElement = list.get(index);
            int nextPosition = closing + 1;

            if (nextPosition < length && segment.charAt(nextPosition) == '[') {
                if (!(currentFiltered.get(index) instanceof List<?>)
                    && originalElement instanceof List<?> nestedOriginal) {

                    List<Object> nestedMutable = createNullFilledList(nestedOriginal.size());

                    currentFiltered.set(index, nestedMutable);
                }

                if (currentFiltered.get(index) instanceof List<?> nestedFiltered) {
                    currentFiltered = (List<Object>) nestedFiltered;
                }

                currentOriginal = originalElement;
                position = nextPosition;
            } else {
                return new ListNavigationResult(currentFiltered, index, originalElement);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static void setValueInList(
        List<Object> filteredList, String segment, int firstBracket, Object value, List<?> originalList) {

        int position = firstBracket;
        int length = segment.length();
        List<Object> currentList = filteredList;
        List<?> currentOriginal = originalList;

        while (position < length && segment.charAt(position) == '[') {
            int closing = segment.indexOf(']', position);

            if (closing == -1) {
                return;
            }

            int index = extractIndexFromBracket(segment, position, closing);

            if (index < 0 || index >= currentList.size()) {
                return;
            }

            int nextPosition = closing + 1;

            if (nextPosition < length && segment.charAt(nextPosition) == '[') {
                if (!(currentList.get(index) instanceof List<?>)) {
                    if (index < currentOriginal.size()
                        && currentOriginal.get(index) instanceof List<?> nestedOriginal) {

                        List<Object> nestedMutable = createNullFilledList(nestedOriginal.size());

                        currentList.set(index, nestedMutable);

                        currentOriginal = nestedOriginal;
                    } else {
                        return;
                    }
                } else {
                    if (index < currentOriginal.size()
                        && currentOriginal.get(index) instanceof List<?> nestedOriginal) {

                        currentOriginal = nestedOriginal;
                    }
                }

                currentList = (List<Object>) currentList.get(index);
                position = nextPosition;
            } else {
                currentList.set(index, value);

                return;
            }
        }
    }

    private static List<Object> createNullFilledList(int size) {
        List<Object> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            list.add(null);
        }

        return list;
    }

    private static int extractIndexFromBracket(String segment, int openPosition, int closePosition) {
        try {
            return Integer.parseInt(segment.substring(openPosition + 1, closePosition));
        } catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    private record ListNavigationResult(List<Object> filteredList, int index, Object originalElement) {
    }

}
