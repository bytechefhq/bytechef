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

package com.bytechef.platform.workflow.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import com.bytechef.platform.workflow.execution.repository.WorkflowExecutionRowRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * @author Ivica Cardic
 */
class WorkflowExecutionRowServiceTest {

    private final WorkflowExecutionRowRepository workflowExecutionRowRepository =
        mock(WorkflowExecutionRowRepository.class);
    private final WorkflowExecutionRowService workflowExecutionRowService =
        new WorkflowExecutionRowServiceImpl(workflowExecutionRowRepository);

    @Test
    void testStatusAndTypeReachTheRepositoryAsOrdinalsWithTheDefaultPageSize() {
        Page<WorkflowExecutionRowDTO> page = new PageImpl<>(
            List.of(new WorkflowExecutionRowDTO(WorkflowExecutionRowDTO.Kind.JOB, 5L)));

        when(workflowExecutionRowRepository.findAll(
            any(), any(), any(), anyList(), anyInt(), anyList(), anyBoolean(), anyList(), any(Pageable.class)))
                .thenReturn(page);

        Page<WorkflowExecutionRowDTO> result = workflowExecutionRowService.getWorkflowExecutionRows(
            Status.FAILED, null, null, List.of(9L), PlatformType.AUTOMATION, List.of("workflow-1"), true,
            List.of("enc-a"), 2);

        ArgumentCaptor<Pageable> pageableArgumentCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(workflowExecutionRowRepository).findAll(
            eq(Status.FAILED.ordinal()), isNull(), isNull(), eq(List.of(9L)), eq(PlatformType.AUTOMATION.ordinal()),
            eq(List.of("workflow-1")), eq(true), eq(List.of("enc-a")), pageableArgumentCaptor.capture());

        assertThat(pageableArgumentCaptor.getValue())
            .isEqualTo(PageRequest.of(2, PrincipalJobRepository.DEFAULT_PAGE_SIZE));
        assertThat(result).isSameAs(page);
    }

    @Test
    void testANullStatusIsPassedThroughAsNull() {
        when(workflowExecutionRowRepository.findAll(
            any(), any(), any(), anyList(), anyInt(), anyList(), anyBoolean(), anyList(), any(Pageable.class)))
                .thenReturn(Page.empty());

        workflowExecutionRowService.getWorkflowExecutionRows(
            null, null, null, List.of(), PlatformType.AUTOMATION, List.of(), true, List.of(), 0);

        verify(workflowExecutionRowRepository).findAll(
            isNull(), isNull(), isNull(), eq(List.of()), eq(PlatformType.AUTOMATION.ordinal()), eq(List.of()),
            eq(true), eq(List.of()), any(Pageable.class));
    }
}
