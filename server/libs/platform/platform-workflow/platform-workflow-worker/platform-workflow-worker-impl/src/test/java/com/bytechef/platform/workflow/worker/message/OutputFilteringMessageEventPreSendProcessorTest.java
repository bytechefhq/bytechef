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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OutputFilteringMessageEventPreSendProcessorTest {

    private OutputFilteringMessageEventPreSendProcessor processor;
    private TaskFileStorage taskFileStorage;

    @BeforeEach
    void beforeEach() {
        taskFileStorage = mock(TaskFileStorage.class);

        processor = new OutputFilteringMessageEventPreSendProcessor(taskFileStorage);
    }

    @Test
    void testProcessFiltersOutputByReferencePaths() {
        Map<String, Object> fullOutput = Map.of(
            "response", Map.of("lastname", "Doe", "firstname", "John", "age", 30),
            "meta", Map.of("more_info", "info@test.com", "status", "active"),
            "extraField", "unused");

        FileEntry originalOutputFileEntry = mock(FileEntry.class);
        FileEntry filteredOutputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(originalOutputFileEntry)).thenReturn(fullOutput);
        when(taskFileStorage.storeTaskExecutionOutput(anyLong(), anyLong(), any())).thenReturn(filteredOutputFileEntry);

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(originalOutputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS,
            Set.of("response.lastname", "meta.more_info"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        MessageEvent<?> result = processor.process(event);

        assertThat(result).isSameAs(event);
        assertThat(taskExecution.getOutput()).isSameAs(filteredOutputFileEntry);

        verify(taskFileStorage).storeTaskExecutionOutput(anyLong(), anyLong(), any());
    }

    @Test
    void testProcessPassesThroughWhenNoReferencePaths() {
        FileEntry outputFileEntry = mock(FileEntry.class);

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(outputFileEntry);

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        MessageEvent<?> result = processor.process(event);

        assertThat(result).isSameAs(event);
        assertThat(taskExecution.getOutput()).isSameAs(outputFileEntry);

        verify(taskFileStorage, never()).readTaskExecutionOutput(any());
    }

    @Test
    void testProcessPassesThroughWhenOutputIsNull() {
        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS, Set.of("response.lastname"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        MessageEvent<?> result = processor.process(event);

        assertThat(result).isSameAs(event);

        verify(taskFileStorage, never()).readTaskExecutionOutput(any());
    }

    @Test
    void testProcessPassesThroughWhenOutputIsNotMap() {
        FileEntry outputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(outputFileEntry)).thenReturn("stringOutput");

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(outputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS, Set.of("response.lastname"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        MessageEvent<?> result = processor.process(event);

        assertThat(result).isSameAs(event);
        assertThat(taskExecution.getOutput()).isSameAs(outputFileEntry);

        verify(taskFileStorage, never()).storeTaskExecutionOutput(anyLong(), anyLong(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessDeepPathFiltering() {
        Map<String, Object> fullOutput = Map.of(
            "response", Map.of("lastname", "Doe", "firstname", "John"),
            "meta", Map.of("more_info", "info@test.com"));

        FileEntry originalOutputFileEntry = mock(FileEntry.class);
        FileEntry filteredOutputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(originalOutputFileEntry)).thenReturn(fullOutput);
        when(taskFileStorage.storeTaskExecutionOutput(anyLong(), anyLong(), any()))
            .thenAnswer(invocation -> {
                Map<String, Object> filteredOutput = (Map<String, Object>) invocation.getArgument(2);

                assertThat(filteredOutput).containsKey("response");
                assertThat(filteredOutput).containsKey("meta");

                Map<String, Object> responseMap = (Map<String, Object>) filteredOutput.get("response");

                assertThat(responseMap).containsEntry("lastname", "Doe");
                assertThat(responseMap).doesNotContainKey("firstname");

                Map<String, Object> metaMap = (Map<String, Object>) filteredOutput.get("meta");

                assertThat(metaMap).containsEntry("more_info", "info@test.com");

                return filteredOutputFileEntry;
            });

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(originalOutputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS,
            Set.of("response.lastname", "meta.more_info"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        processor.process(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessFiltersSingleLevelPropertyPaths() {
        Map<String, Object> fullOutput = Map.of(
            "name", "John Doe",
            "status", "active",
            "age", 30,
            "email", "john@test.com");

        FileEntry originalOutputFileEntry = mock(FileEntry.class);
        FileEntry filteredOutputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(originalOutputFileEntry)).thenReturn(fullOutput);
        when(taskFileStorage.storeTaskExecutionOutput(anyLong(), anyLong(), any()))
            .thenAnswer(invocation -> {
                Map<String, Object> filteredOutput = (Map<String, Object>) invocation.getArgument(2);

                assertThat(filteredOutput).containsEntry("name", "John Doe");
                assertThat(filteredOutput).containsEntry("status", "active");
                assertThat(filteredOutput).doesNotContainKey("age");
                assertThat(filteredOutput).doesNotContainKey("email");

                return filteredOutputFileEntry;
            });

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(originalOutputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS, Set.of("name", "status"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        processor.process(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessFiltersWithArrayValueAtLeaf() {
        Map<String, Object> fullOutput = Map.of(
            "tags", List.of("tag1", "tag2", "tag3"),
            "response", Map.of("items", List.of("a", "b"), "count", 2),
            "unused", "data");

        FileEntry originalOutputFileEntry = mock(FileEntry.class);
        FileEntry filteredOutputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(originalOutputFileEntry)).thenReturn(fullOutput);
        when(taskFileStorage.storeTaskExecutionOutput(anyLong(), anyLong(), any()))
            .thenAnswer(invocation -> {
                Map<String, Object> filteredOutput = (Map<String, Object>) invocation.getArgument(2);

                assertThat(filteredOutput).containsEntry("tags", List.of("tag1", "tag2", "tag3"));

                Map<String, Object> responseMap = (Map<String, Object>) filteredOutput.get("response");

                assertThat(responseMap).containsEntry("items", List.of("a", "b"));
                assertThat(responseMap).doesNotContainKey("count");
                assertThat(filteredOutput).doesNotContainKey("unused");

                return filteredOutputFileEntry;
            });

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(originalOutputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS, Set.of("tags", "response.items"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        processor.process(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessFiltersWithNonExistentPath() {
        Map<String, Object> fullOutput = Map.of(
            "response", Map.of("lastname", "Doe"));

        FileEntry originalOutputFileEntry = mock(FileEntry.class);
        FileEntry filteredOutputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(originalOutputFileEntry)).thenReturn(fullOutput);
        when(taskFileStorage.storeTaskExecutionOutput(anyLong(), anyLong(), any()))
            .thenAnswer(invocation -> {
                Map<String, Object> filteredOutput = (Map<String, Object>) invocation.getArgument(2);

                assertThat(filteredOutput).containsKey("response");

                Map<String, Object> responseMap = (Map<String, Object>) filteredOutput.get("response");

                assertThat(responseMap).containsEntry("lastname", "Doe");
                assertThat(filteredOutput).doesNotContainKey("nonexistent");

                return filteredOutputFileEntry;
            });

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(originalOutputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS,
            Set.of("response.lastname", "nonexistent.field"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        processor.process(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessFiltersDeeplyNestedPaths() {
        Map<String, Object> fullOutput = Map.of(
            "response", Map.of(
                "contact", Map.of(
                    "address", Map.of("city", "New York", "zip", "10001"),
                    "phone", "555-1234")),
            "meta", Map.of("status", "ok"));

        FileEntry originalOutputFileEntry = mock(FileEntry.class);
        FileEntry filteredOutputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(originalOutputFileEntry)).thenReturn(fullOutput);
        when(taskFileStorage.storeTaskExecutionOutput(anyLong(), anyLong(), any()))
            .thenAnswer(invocation -> {
                Map<String, Object> filteredOutput = (Map<String, Object>) invocation.getArgument(2);

                Map<String, Object> responseMap = (Map<String, Object>) filteredOutput.get("response");
                Map<String, Object> contactMap = (Map<String, Object>) responseMap.get("contact");
                Map<String, Object> addressMap = (Map<String, Object>) contactMap.get("address");

                assertThat(addressMap).containsEntry("city", "New York");
                assertThat(addressMap).doesNotContainKey("zip");
                assertThat(contactMap).doesNotContainKey("phone");
                assertThat(filteredOutput).doesNotContainKey("meta");

                return filteredOutputFileEntry;
            });

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(originalOutputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS,
            Set.of("response.contact.address.city"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        processor.process(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessFiltersPrimitiveValues() {
        Map<String, Object> fullOutput = Map.of(
            "count", 42,
            "active", true,
            "ratio", 2.75,
            "name", "test",
            "unused", "data");

        FileEntry originalOutputFileEntry = mock(FileEntry.class);
        FileEntry filteredOutputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(originalOutputFileEntry)).thenReturn(fullOutput);
        when(taskFileStorage.storeTaskExecutionOutput(anyLong(), anyLong(), any()))
            .thenAnswer(invocation -> {
                Map<String, Object> filteredOutput = (Map<String, Object>) invocation.getArgument(2);

                assertThat(filteredOutput).containsEntry("count", 42);
                assertThat(filteredOutput).containsEntry("active", true);
                assertThat(filteredOutput).containsEntry("ratio", 2.75);
                assertThat(filteredOutput).doesNotContainKey("name");
                assertThat(filteredOutput).doesNotContainKey("unused");

                return filteredOutputFileEntry;
            });

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(originalOutputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS,
            Set.of("count", "active", "ratio"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        processor.process(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessFiltersWithArrayIndexedPaths() {
        Map<String, Object> fullOutput = Map.of(
            "items", List.of(
                Map.of("name", "First", "value", 10),
                Map.of("name", "Second", "value", 20)),
            "response", Map.of(
                "elements", List.of(
                    Map.of("propBool", true, "propNumber", 3.5))),
            "unused", "data");

        FileEntry originalOutputFileEntry = mock(FileEntry.class);
        FileEntry filteredOutputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(originalOutputFileEntry)).thenReturn(fullOutput);
        when(taskFileStorage.storeTaskExecutionOutput(anyLong(), anyLong(), any()))
            .thenAnswer(invocation -> {
                Map<String, Object> filteredOutput = (Map<String, Object>) invocation.getArgument(2);

                assertThat(filteredOutput).containsKey("items");
                assertThat(filteredOutput.get("items")).isInstanceOf(List.class);

                List<Object> itemsList = (List<Object>) filteredOutput.get("items");

                assertThat(itemsList).hasSize(2);

                Map<String, Object> firstItem = (Map<String, Object>) itemsList.get(0);

                assertThat(firstItem).containsEntry("name", "First");
                assertThat(firstItem).doesNotContainKey("value");

                assertThat(itemsList.get(1)).isNull();

                assertThat(filteredOutput).containsKey("response");

                Map<String, Object> responseMap = (Map<String, Object>) filteredOutput.get("response");

                assertThat(responseMap).containsKey("elements");
                assertThat(responseMap.get("elements")).isInstanceOf(List.class);

                List<Object> elementsList = (List<Object>) responseMap.get("elements");

                assertThat(elementsList).hasSize(1);

                Map<String, Object> firstElement = (Map<String, Object>) elementsList.get(0);

                assertThat(firstElement).containsEntry("propBool", true);

                assertThat(filteredOutput).doesNotContainKey("unused");

                return filteredOutputFileEntry;
            });

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(originalOutputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS,
            Set.of("items[0].name", "response.elements[0].propBool"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        processor.process(event);
    }

    @Test
    void testProcessPassesThroughWhenOutputIsList() {
        FileEntry outputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(outputFileEntry)).thenReturn(List.of("item1", "item2"));

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(outputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS, Set.of("response.lastname"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        MessageEvent<?> result = processor.process(event);

        assertThat(result).isSameAs(event);
        assertThat(taskExecution.getOutput()).isSameAs(outputFileEntry);

        verify(taskFileStorage, never()).storeTaskExecutionOutput(anyLong(), anyLong(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessFiltersWithOverlappingParentAndChildPaths() {
        Map<String, Object> fullOutput = Map.of(
            "response", Map.of("lastname", "Doe", "firstname", "John", "age", 30),
            "unused", "data");

        FileEntry originalOutputFileEntry = mock(FileEntry.class);
        FileEntry filteredOutputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(originalOutputFileEntry)).thenReturn(fullOutput);
        when(taskFileStorage.storeTaskExecutionOutput(anyLong(), anyLong(), any()))
            .thenAnswer(invocation -> {
                Map<String, Object> filteredOutput = (Map<String, Object>) invocation.getArgument(2);

                assertThat(filteredOutput).containsKey("response");

                Map<String, Object> responseMap = (Map<String, Object>) filteredOutput.get("response");

                assertThat(responseMap).containsEntry("lastname", "Doe");
                assertThat(responseMap).containsEntry("firstname", "John");
                assertThat(responseMap).containsEntry("age", 30);
                assertThat(filteredOutput).doesNotContainKey("unused");

                return filteredOutputFileEntry;
            });

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(originalOutputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS,
            Set.of("response", "response.lastname"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        processor.process(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessFiltersWithMultiBracketIndexedPaths() {
        Map<String, Object> fullOutput = Map.of(
            "conditions", List.of(
                List.of(
                    Map.of("value", "first", "type", "text"),
                    Map.of("value", "second", "type", "number")),
                List.of(
                    Map.of("value", "third", "type", "bool"))));

        FileEntry originalOutputFileEntry = mock(FileEntry.class);
        FileEntry filteredOutputFileEntry = mock(FileEntry.class);

        when(taskFileStorage.readTaskExecutionOutput(originalOutputFileEntry)).thenReturn(fullOutput);
        when(taskFileStorage.storeTaskExecutionOutput(anyLong(), anyLong(), any()))
            .thenAnswer(invocation -> {
                Map<String, Object> filteredOutput = (Map<String, Object>) invocation.getArgument(2);

                assertThat(filteredOutput).containsKey("conditions");
                assertThat(filteredOutput.get("conditions")).isInstanceOf(List.class);

                List<Object> conditionsList = (List<Object>) filteredOutput.get("conditions");

                assertThat(conditionsList).hasSize(2);

                Object firstRow = conditionsList.get(0);

                assertThat(firstRow).isInstanceOf(List.class);

                List<Object> firstRowList = (List<Object>) firstRow;

                assertThat(firstRowList).hasSize(2);

                Map<String, Object> secondElement = (Map<String, Object>) firstRowList.get(1);

                assertThat(secondElement).containsEntry("value", "second");

                return filteredOutputFileEntry;
            });

        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.setOutput(originalOutputFileEntry);
        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS,
            Set.of("conditions[0][1].value"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        processor.process(event);
    }
}
