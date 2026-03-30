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

package com.bytechef.automation.task.facade;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.dto.ApprovalTaskDTO;
import com.bytechef.automation.task.service.ApprovalTaskService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ApprovalTaskFacadeImpl implements ApprovalTaskFacade {

    private final ApprovalTaskService approvalTaskService;

    @SuppressFBWarnings("EI")
    public ApprovalTaskFacadeImpl(ApprovalTaskService approvalTaskService) {
        this.approvalTaskService = approvalTaskService;
    }

    @Override
    public long countApprovalTasks() {
        return approvalTaskService.countApprovalTasks();
    }

    @Override
    public ApprovalTaskDTO createApprovalTask(ApprovalTaskDTO approvalTaskDTO) {
        ApprovalTask approvalTask = toApprovalTask(approvalTaskDTO);

        return toApprovalTaskDTO(approvalTaskService.create(approvalTask));
    }

    @Override
    public void deleteApprovalTask(long id) {
        approvalTaskService.delete(id);
    }

    @Override
    public Optional<ApprovalTaskDTO> fetchApprovalTask(String name) {
        return approvalTaskService.fetchApprovalTask(name)
            .map(this::toApprovalTaskDTO);
    }

    @Override
    public ApprovalTaskDTO getApprovalTask(long id) {
        return toApprovalTaskDTO(approvalTaskService.getApprovalTask(id));
    }

    @Override
    public List<ApprovalTaskDTO> getApprovalTasks() {
        return approvalTaskService.getApprovalTasks()
            .stream()
            .map(this::toApprovalTaskDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<ApprovalTaskDTO> getApprovalTasks(List<Long> ids) {
        return approvalTaskService.getApprovalTasks(ids)
            .stream()
            .map(this::toApprovalTaskDTO)
            .collect(Collectors.toList());
    }

    @Override
    public ApprovalTaskDTO updateApprovalTask(ApprovalTaskDTO approvalTaskDTO) {
        ApprovalTask approvalTask = toApprovalTask(approvalTaskDTO);

        return toApprovalTaskDTO(approvalTaskService.update(approvalTask));
    }

    private ApprovalTask toApprovalTask(ApprovalTaskDTO approvalTaskDTO) {
        return ApprovalTask.builder()
            .id(approvalTaskDTO.id())
            .name(approvalTaskDTO.name())
            .description(approvalTaskDTO.description())
            .version(approvalTaskDTO.version())
            .build();
    }

    private ApprovalTaskDTO toApprovalTaskDTO(ApprovalTask approvalTask) {
        return new ApprovalTaskDTO(
            approvalTask.getCreatedBy(),
            approvalTask.getCreatedDate(),
            approvalTask.getDescription(),
            approvalTask.getId(),
            approvalTask.getLastModifiedBy(),
            approvalTask.getLastModifiedDate(),
            approvalTask.getName(),
            approvalTask.getVersion());
    }
}
