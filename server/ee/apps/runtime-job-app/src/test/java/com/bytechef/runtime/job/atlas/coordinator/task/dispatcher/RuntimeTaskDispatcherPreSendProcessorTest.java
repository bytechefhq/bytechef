/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.atlas.coordinator.task.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.runtime.job.platform.connection.ConnectionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.jackson.JsonComponentModule;

/**
 * @version ee
 *
 *          Unit tests for {@link RuntimeTaskDispatcherPreSendProcessor}.
 *
 * @author Ivica Cardic
 */
class RuntimeTaskDispatcherPreSendProcessorTest {

    private ApplicationArguments applicationArguments;
    private RuntimeTaskDispatcherPreSendProcessor processor;

    @BeforeAll
    static void setUpUtils() {
        JacksonConfiguration jacksonConfiguration = new JacksonConfiguration(new JsonComponentModule());

        JsonUtils.setObjectMapper(jacksonConfiguration.objectMapper());
        MapUtils.setObjectMapper(jacksonConfiguration.objectMapper());
    }

    @BeforeEach
    void beforeEach() {
        applicationArguments = mock(ApplicationArguments.class);

        List<String> connectionValues = List.of(
            "{\"testTask\": {\"apiKey\": \"test-api-key\", \"url\": \"https://test.com\"}}",
            "{\"http\": {\"timeout\": \"30000\", \"retries\": \"3\"}}");

        when(applicationArguments.getOptionValues("connections")).thenReturn(connectionValues);

        processor = new RuntimeTaskDispatcherPreSendProcessor(applicationArguments);
    }

    @Test
    void testProcessWithTaskNameMatch() {
        // Given
        WorkflowTask workflowTask = createWorkflowTask("testTask", "http/v1/get");

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(workflowTask)
            .build();

        // When
        TaskExecution result = processor.process(taskExecution);

        // Then
        assertThat(result).isNotNull();

        Map<String, ?> metadata = result.getMetadata();

        assertThat(metadata).containsKey(MetadataConstants.CONNECTION_IDS);

        @SuppressWarnings("unchecked")
        Map<String, Long> connectionIds = (Map<String, Long>) metadata.get(MetadataConstants.CONNECTION_IDS);

        assertThat(connectionIds).containsKey("testTask");

        Long connectionId = connectionIds.get("testTask");

        assertThat(connectionId).isNotNull();

        // Verify connection parameters are stored correctly
        Map<String, ?> storedParameters = ConnectionContext.getConnectionParameters(connectionId);

        assertThat(storedParameters).containsKey("apiKey");
        assertThat(storedParameters).containsKey("url");
        assertThat(storedParameters.get("apiKey")).isEqualTo("test-api-key");
        assertThat(storedParameters.get("url")).isEqualTo("https://test.com");
    }

    @Test
    void testProcessWithComponentNameMatch() {
        // Given
        WorkflowTask workflowTask = createWorkflowTask("someOtherTask", "http/v1/get");

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(workflowTask)
            .build();

        // When
        TaskExecution result = processor.process(taskExecution);

        // Then
        assertThat(result).isNotNull();

        Map<String, ?> metadata = result.getMetadata();

        assertThat(metadata).containsKey(MetadataConstants.CONNECTION_IDS);

        @SuppressWarnings("unchecked")
        Map<String, Long> connectionIds = (Map<String, Long>) metadata.get(MetadataConstants.CONNECTION_IDS);

        assertThat(connectionIds).containsKey("someOtherTask");

        Long connectionId = connectionIds.get("someOtherTask");

        assertThat(connectionId).isNotNull();

        // Verify connection parameters are stored correctly (should match HTTP component)
        Map<String, ?> storedParameters = ConnectionContext.getConnectionParameters(connectionId);

        assertThat(storedParameters).containsKey("timeout");
        assertThat(storedParameters).containsKey("retries");
        assertThat(storedParameters.get("timeout")).isEqualTo("30000");
        assertThat(storedParameters.get("retries")).isEqualTo("3");
    }

    @Test
    void testProcessWithNoConnectionMatch() {
        // Given
        WorkflowTask workflowTask = createWorkflowTask("unknownTask", "unknown/v1/run");

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(workflowTask)
            .build();

        // When
        TaskExecution result = processor.process(taskExecution);

        // Then
        assertThat(result).isNotNull();
        // Should not have connection IDs metadata when no match is found
        assertThat(result.getMetadata()).doesNotContainKey(MetadataConstants.CONNECTION_IDS);
    }

    @Test
    void testProcessWithExistingMetadata() {
        // Given
        WorkflowTask workflowTask = createWorkflowTask("testTask", "http/v1/get");
        Map<String, Object> existingMetadata = new HashMap<>();

        existingMetadata.put("existingKey", "existingValue");

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(workflowTask)
            .metadata(existingMetadata)
            .build();

        // When
        TaskExecution result = processor.process(taskExecution);

        // Then
        assertThat(result).isNotNull();

        Map<String, ?> metadata = result.getMetadata();

        assertThat(metadata).containsKey("existingKey");
        assertThat(metadata).containsKey(MetadataConstants.CONNECTION_IDS);
        assertThat(metadata.get("existingKey")).isEqualTo("existingValue");
    }

    @Test
    void testCanProcessAlwaysReturnsTrue() {
        // Given
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        // When
        boolean canProcess = processor.canProcess(taskExecution);

        // Then
        assertThat(canProcess).isTrue();
    }

    @Test
    void testProcessWithEmptyConnectionParameters() {
        // Given - setup processor with empty connections
        when(applicationArguments.getOptionValues("connections")).thenReturn(List.of("{}"));

        RuntimeTaskDispatcherPreSendProcessor emptyProcessor = new RuntimeTaskDispatcherPreSendProcessor(
            applicationArguments);

        WorkflowTask workflowTask = createWorkflowTask("testTask", "http/v1/get");

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(workflowTask)
            .build();

        // When
        TaskExecution result = emptyProcessor.process(taskExecution);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMetadata()).doesNotContainKey(MetadataConstants.CONNECTION_IDS);
    }

    @Test
    void testProcessWithMultipleConnectionMaps() {
        // Given - setup processor with multiple connection maps
        List<String> connectionValues = List.of(
            "{\"task1\": {\"key1\": \"value1\"}}",
            "{\"task2\": {\"key2\": \"value2\"}}",
            "{\"task1\": {\"key3\": \"value3\"}}" // This should merge with first task1
        );

        when(applicationArguments.getOptionValues("connections")).thenReturn(connectionValues);

        RuntimeTaskDispatcherPreSendProcessor multiProcessor = new RuntimeTaskDispatcherPreSendProcessor(
            applicationArguments);

        WorkflowTask workflowTask = createWorkflowTask("task2", "http/v1/get");

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(workflowTask)
            .build();

        // When
        TaskExecution result = multiProcessor.process(taskExecution);

        // Then
        assertThat(result).isNotNull();

        Map<String, ?> metadata = result.getMetadata();

        assertThat(metadata).containsKey(MetadataConstants.CONNECTION_IDS);

        @SuppressWarnings("unchecked")
        Map<String, Long> connectionIds = (Map<String, Long>) metadata.get(MetadataConstants.CONNECTION_IDS);

        assertThat(connectionIds).containsKey("task2");

        Long connectionId = connectionIds.get("task2");

        Map<String, ?> storedParameters = ConnectionContext.getConnectionParameters(connectionId);

        assertThat(storedParameters).containsKey("key2");
        assertThat(storedParameters.get("key2")).isEqualTo("value2");
    }

    @Test
    void testConnectionContextReusesExistingConnection() {
        // Given
        WorkflowTask workflowTask1 = createWorkflowTask("testTask", "http/v1/get");

        TaskExecution taskExecution1 = TaskExecution.builder()
            .workflowTask(workflowTask1)
            .build();

        WorkflowTask workflowTask2 = createWorkflowTask("testTask", "http/v1/get");

        TaskExecution taskExecution2 = TaskExecution.builder()
            .workflowTask(workflowTask2)
            .build();

        // When
        TaskExecution result1 = processor.process(taskExecution1);
        TaskExecution result2 = processor.process(taskExecution2);

        // Then
        Map<String, ?> metadata1 = result1.getMetadata();

        @SuppressWarnings("unchecked")
        Map<String, Long> connectionIds1 = (Map<String, Long>) metadata1.get(MetadataConstants.CONNECTION_IDS);
        Map<String, ?> metadata2 = result2.getMetadata();

        @SuppressWarnings("unchecked")
        Map<String, Long> connectionIds2 = (Map<String, Long>) metadata2.get(MetadataConstants.CONNECTION_IDS);

        // Should reuse the same connection ID for the same connection name
        assertThat(connectionIds1.get("testTask")).isEqualTo(connectionIds2.get("testTask"));
    }

    private WorkflowTask createWorkflowTask(String name, String type) {
        Map<String, Object> taskMap = new HashMap<>();

        taskMap.put("name", name);
        taskMap.put("type", type);

        return new WorkflowTask(taskMap);
    }
}
