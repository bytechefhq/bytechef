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

package com.bytechef.platform.workflow.coordinator.task.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.RandomUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OutputReferenceTaskDispatcherPreSendProcessorTest {

    private JobService jobService;
    private OutputReferenceTaskDispatcherPreSendProcessor processor;
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        jobService = mock(JobService.class);
        workflowService = mock(WorkflowService.class);

        processor = new OutputReferenceTaskDispatcherPreSendProcessor(jobService, workflowService);
    }

    @Test
    void testCanProcessWithNamedTask() {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        assertThat(processor.canProcess(taskExecution)).isTrue();
    }

    @Test
    void testProcessAttachesReferencePaths() {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(RandomUtils.nextLong())
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        String workflowDefinition = """
            {
                "label": "test",
                "tasks": [
                    {
                        "name": "accelo_1",
                        "type": "accelo/v1/createContact",
                        "parameters": {}
                    },
                    {
                        "name": "activeCampaign_1",
                        "type": "activeCampaign/v1/createContact",
                        "parameters": {
                            "phone": "${accelo_1.response.lastname}",
                            "email": "${accelo_1.meta.more_info}"
                        }
                    }
                ]
            }
            """;

        Workflow workflow = new Workflow("testWorkflow", workflowDefinition, Workflow.Format.JSON);

        Job job = new Job();

        job.setWorkflowId("testWorkflow");

        when(jobService.getJob(anyLong())).thenReturn(job);
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        Object referencePaths = metadata.get(MetadataConstants.OUTPUT_REFERENCE_PATHS);

        assertThat(referencePaths).isInstanceOf(Collection.class);

        @SuppressWarnings("unchecked")
        Collection<String> referencePathCollection = (Collection<String>) referencePaths;

        assertThat(referencePathCollection).containsExactlyInAnyOrder("response.lastname", "meta.more_info");
    }

    @Test
    void testProcessWithNoDownstreamReferences() {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(RandomUtils.nextLong())
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        String workflowDefinition = """
            {
                "label": "test",
                "tasks": [
                    {
                        "name": "accelo_1",
                        "type": "accelo/v1/createContact",
                        "parameters": {}
                    },
                    {
                        "name": "activeCampaign_1",
                        "type": "activeCampaign/v1/createContact",
                        "parameters": {
                            "phone": "staticValue"
                        }
                    }
                ]
            }
            """;

        Workflow workflow = new Workflow("testWorkflow", workflowDefinition, Workflow.Format.JSON);

        Job job = new Job();

        job.setWorkflowId("testWorkflow");

        when(jobService.getJob(anyLong())).thenReturn(job);
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        assertThat(metadata.get(MetadataConstants.OUTPUT_REFERENCE_PATHS)).isNull();
    }

    @Test
    void testProcessWithArrayParameters() {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(RandomUtils.nextLong())
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        String workflowDefinition = """
            {
                "label": "test",
                "tasks": [
                    {
                        "name": "accelo_1",
                        "type": "accelo/v1/createContact",
                        "parameters": {}
                    },
                    {
                        "name": "activeCampaign_1",
                        "type": "activeCampaign/v1/createContact",
                        "parameters": {
                            "tags": ["${accelo_1.tags.primary}", "${accelo_1.tags.secondary}"],
                            "phone": "staticValue"
                        }
                    }
                ]
            }
            """;

        Workflow workflow = new Workflow("testWorkflow", workflowDefinition, Workflow.Format.JSON);

        Job job = new Job();

        job.setWorkflowId("testWorkflow");

        when(jobService.getJob(anyLong())).thenReturn(job);
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        Object referencePaths = metadata.get(MetadataConstants.OUTPUT_REFERENCE_PATHS);

        assertThat(referencePaths).isInstanceOf(Collection.class);

        @SuppressWarnings("unchecked")
        Collection<String> referencePathCollection = (Collection<String>) referencePaths;

        assertThat(referencePathCollection).containsExactlyInAnyOrder("tags.primary", "tags.secondary");
    }

    @Test
    void testProcessWithBareTaskReference() {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(RandomUtils.nextLong())
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        String workflowDefinition = """
            {
                "label": "test",
                "tasks": [
                    {
                        "name": "accelo_1",
                        "type": "accelo/v1/createContact",
                        "parameters": {}
                    },
                    {
                        "name": "activeCampaign_1",
                        "type": "activeCampaign/v1/createContact",
                        "parameters": {
                            "input": "${accelo_1}"
                        }
                    }
                ]
            }
            """;

        Workflow workflow = new Workflow("testWorkflow", workflowDefinition, Workflow.Format.JSON);

        Job job = new Job();

        job.setWorkflowId("testWorkflow");

        when(jobService.getJob(anyLong())).thenReturn(job);
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        assertThat(metadata.get(MetadataConstants.OUTPUT_REFERENCE_PATHS)).isNull();
    }

    @Test
    void testProcessWithNestedMapParameters() {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(RandomUtils.nextLong())
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        String workflowDefinition = """
            {
                "label": "test",
                "tasks": [
                    {
                        "name": "accelo_1",
                        "type": "accelo/v1/createContact",
                        "parameters": {}
                    },
                    {
                        "name": "activeCampaign_1",
                        "type": "activeCampaign/v1/createContact",
                        "parameters": {
                            "contact": {
                                "name": "${accelo_1.response.fullname}",
                                "address": {
                                    "city": "${accelo_1.address.city}"
                                }
                            }
                        }
                    }
                ]
            }
            """;

        Workflow workflow = new Workflow("testWorkflow", workflowDefinition, Workflow.Format.JSON);

        Job job = new Job();

        job.setWorkflowId("testWorkflow");

        when(jobService.getJob(anyLong())).thenReturn(job);
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        Object referencePaths = metadata.get(MetadataConstants.OUTPUT_REFERENCE_PATHS);

        assertThat(referencePaths).isInstanceOf(Collection.class);

        @SuppressWarnings("unchecked")
        Collection<String> referencePathCollection = (Collection<String>) referencePaths;

        assertThat(referencePathCollection).containsExactlyInAnyOrder("response.fullname", "address.city");
    }

    @Test
    void testProcessWithMultipleReferencesInSingleString() {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(RandomUtils.nextLong())
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        String workflowDefinition = """
            {
                "label": "test",
                "tasks": [
                    {
                        "name": "accelo_1",
                        "type": "accelo/v1/createContact",
                        "parameters": {}
                    },
                    {
                        "name": "activeCampaign_1",
                        "type": "activeCampaign/v1/createContact",
                        "parameters": {
                            "fullName": "${accelo_1.response.firstname} ${accelo_1.response.lastname}"
                        }
                    }
                ]
            }
            """;

        Workflow workflow = new Workflow("testWorkflow", workflowDefinition, Workflow.Format.JSON);

        Job job = new Job();

        job.setWorkflowId("testWorkflow");

        when(jobService.getJob(anyLong())).thenReturn(job);
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        Object referencePaths = metadata.get(MetadataConstants.OUTPUT_REFERENCE_PATHS);

        assertThat(referencePaths).isInstanceOf(Collection.class);

        @SuppressWarnings("unchecked")
        Collection<String> referencePathCollection = (Collection<String>) referencePaths;

        assertThat(referencePathCollection).containsExactlyInAnyOrder("response.firstname", "response.lastname");
    }

    @Test
    void testProcessWithSingleLevelPropertyPath() {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(RandomUtils.nextLong())
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        String workflowDefinition = """
            {
                "label": "test",
                "tasks": [
                    {
                        "name": "accelo_1",
                        "type": "accelo/v1/createContact",
                        "parameters": {}
                    },
                    {
                        "name": "activeCampaign_1",
                        "type": "activeCampaign/v1/createContact",
                        "parameters": {
                            "name": "${accelo_1.name}",
                            "status": "${accelo_1.status}"
                        }
                    }
                ]
            }
            """;

        Workflow workflow = new Workflow("testWorkflow", workflowDefinition, Workflow.Format.JSON);

        Job job = new Job();

        job.setWorkflowId("testWorkflow");

        when(jobService.getJob(anyLong())).thenReturn(job);
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        Object referencePaths = metadata.get(MetadataConstants.OUTPUT_REFERENCE_PATHS);

        assertThat(referencePaths).isInstanceOf(Collection.class);

        @SuppressWarnings("unchecked")
        Collection<String> referencePathCollection = (Collection<String>) referencePaths;

        assertThat(referencePathCollection).containsExactlyInAnyOrder("name", "status");
    }

    @Test
    void testProcessWithMixedArrayAndMapParameters() {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(RandomUtils.nextLong())
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        String workflowDefinition = """
            {
                "label": "test",
                "tasks": [
                    {
                        "name": "accelo_1",
                        "type": "accelo/v1/createContact",
                        "parameters": {}
                    },
                    {
                        "name": "activeCampaign_1",
                        "type": "activeCampaign/v1/createContact",
                        "parameters": {
                            "contacts": [
                                {
                                    "name": "${accelo_1.response.fullname}",
                                    "email": "${accelo_1.contact.email}"
                                }
                            ],
                            "phone": "${accelo_1.phone}"
                        }
                    }
                ]
            }
            """;

        Workflow workflow = new Workflow("testWorkflow", workflowDefinition, Workflow.Format.JSON);

        Job job = new Job();

        job.setWorkflowId("testWorkflow");

        when(jobService.getJob(anyLong())).thenReturn(job);
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        Object referencePaths = metadata.get(MetadataConstants.OUTPUT_REFERENCE_PATHS);

        assertThat(referencePaths).isInstanceOf(Collection.class);

        @SuppressWarnings("unchecked")
        Collection<String> referencePathCollection = (Collection<String>) referencePaths;

        assertThat(referencePathCollection).containsExactlyInAnyOrder(
            "response.fullname", "contact.email", "phone");
    }
}
