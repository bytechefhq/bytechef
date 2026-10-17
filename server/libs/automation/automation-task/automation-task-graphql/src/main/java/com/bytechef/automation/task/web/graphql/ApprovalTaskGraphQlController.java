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

package com.bytechef.automation.task.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.ApprovalTask.Priority;
import com.bytechef.automation.task.domain.ApprovalTask.Status;
import com.bytechef.automation.task.domain.ApprovalTaskStatusException;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.facade.ApprovalTaskApiFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ApprovalTaskGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskGraphQlController.class);

    private final ApprovalTaskApiFacade approvalTaskApiFacade;

    @SuppressFBWarnings("EI")
    public ApprovalTaskGraphQlController(ApprovalTaskApiFacade approvalTaskApiFacade) {
        this.approvalTaskApiFacade = approvalTaskApiFacade;
    }

    @MutationMapping
    public ApprovalTask createApprovalTask(@Argument ApprovalTaskInput approvalTask) {
        return approvalTaskApiFacade.createApprovalTask(toApprovalTask(approvalTask));
    }

    @MutationMapping
    public boolean deleteApprovalTask(@Argument long id) {
        approvalTaskApiFacade.deleteApprovalTask(id);

        return true;
    }

    @QueryMapping
    public ApprovalTask approvalTask(@Argument long id) {
        return approvalTaskApiFacade.getApprovalTask(id);
    }

    @QueryMapping
    public List<ApprovalTask> approvalTasks(@Argument Integer environmentId) {
        return approvalTaskApiFacade.getApprovalTasks(environmentId);
    }

    @QueryMapping
    public List<ApprovalTask> approvalTasksByIds(@Argument List<Long> ids) {
        return approvalTaskApiFacade.getApprovalTasks(ids);
    }

    @QueryMapping
    public List<PendingApproval> pendingApprovals(@Argument Integer environmentId) {
        return approvalTaskApiFacade.getPendingApprovals(environmentId);
    }

    @MutationMapping
    public ApprovalTask updateApprovalTask(
        @Argument ApprovalTaskInput approvalTask, DataFetchingEnvironment dataFetchingEnvironment) {

        Map<String, Object> approvalTaskArgument = dataFetchingEnvironment.getArgument("approvalTask");

        Set<String> providedFieldNames = approvalTaskArgument == null ? Set.of() : approvalTaskArgument.keySet();

        ApprovalTask currentApprovalTask = approvalTaskApiFacade.getApprovalTask(
            Objects.requireNonNull(approvalTask.id(), "'id' must not be null"));

        ApprovalTask patchedApprovalTask = ApprovalTask.builder()
            .id(approvalTask.id())
            .name(providedFieldNames.contains("name") ? approvalTask.name() : currentApprovalTask.getName())
            .description(
                providedFieldNames.contains("description")
                    ? approvalTask.description() : currentApprovalTask.getDescription())
            .status(providedFieldNames.contains("status") ? approvalTask.status() : currentApprovalTask.getStatus())
            .priority(
                providedFieldNames.contains("priority") ? approvalTask.priority() : currentApprovalTask.getPriority())
            .assigneeId(
                providedFieldNames.contains("assigneeId")
                    ? approvalTask.assigneeId() : currentApprovalTask.getAssigneeId())
            .dueDate(
                providedFieldNames.contains("dueDate")
                    ? toInstant(approvalTask.dueDate()) : currentApprovalTask.getDueDate())
            .version(
                providedFieldNames.contains("version") && approvalTask.version() != null
                    ? approvalTask.version() : currentApprovalTask.getVersion())
            .build();

        return approvalTaskApiFacade.updateApprovalTask(patchedApprovalTask);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleApprovalTaskStatusException(
        ApprovalTaskStatusException approvalTaskStatusException, DataFetchingEnvironment dataFetchingEnvironment) {

        log.debug("Rejected approval task status change: {}", approvalTaskStatusException.getMessage());

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(ErrorType.BAD_REQUEST)
            .message(approvalTaskStatusException.getMessage())
            .build();
    }

    @Nullable
    private static Instant toInstant(@Nullable String dueDate) {
        return dueDate == null ? null : Instant.parse(dueDate);
    }

    private ApprovalTask toApprovalTask(ApprovalTaskInput approvalTaskInput) {
        return ApprovalTask.builder()
            .id(approvalTaskInput.id())
            .name(approvalTaskInput.name())
            .description(approvalTaskInput.description())
            .status(approvalTaskInput.status())
            .priority(approvalTaskInput.priority())
            .assigneeId(approvalTaskInput.assigneeId())
            .dueDate(toInstant(approvalTaskInput.dueDate()))
            .version(approvalTaskInput.version() == null ? 0 : approvalTaskInput.version())
            .build();
    }

    public record ApprovalTaskInput(
        Long id, String name, String description, Status status, Priority priority, Long assigneeId, String dueDate,
        Integer version) {
    }
}
