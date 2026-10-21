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

package com.bytechef.automation.configuration.workflow;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.util.WorkflowTaskUtils;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * A scheduled workflow is triggered by a schedule trigger and writes its results to a data table.
 *
 * @author Ivica Cardic
 */
@Component
public class ScheduledWorkflowChecker implements WorkflowKindChecker {

    static final String NO_DATA_TABLE_WRITE_TASK = "no dataTable write task";

    private static final String DATA_TABLE = "dataTable";
    private static final Set<String> DATA_TABLE_WRITE_OPERATIONS = Set.of("createRecords", "updateRecord");
    private static final String SCHEDULE = "schedule";
    private static final Set<String> SCHEDULE_OPERATIONS = Set.of(
        "cron", "everyDay", "everyWeek", "everyMonth", "interval");

    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ScheduledWorkflowChecker(ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public WorkflowKind kind() {
        return WorkflowKind.SCHEDULED;
    }

    @Override
    public CallableWorkflowCheck check(String workflowId) {
        Optional<Workflow> fetchedWorkflow = workflowService.fetchWorkflow(workflowId);

        if (fetchedWorkflow.isEmpty()) {
            return new CallableWorkflowCheck(List.of("workflow %s does not exist".formatted(workflowId)), null);
        }

        Workflow workflow = fetchedWorkflow.get();
        List<String> problems = new ArrayList<>();

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        if (workflowTriggers.isEmpty()) {
            problems.add("trigger is none, not a schedule trigger");
        } else {
            WorkflowTrigger workflowTrigger = workflowTriggers.getFirst();

            if (!isOperation(workflowTrigger.getType(), SCHEDULE, SCHEDULE_OPERATIONS)) {
                problems.add("trigger is %s, not a schedule trigger".formatted(workflowTrigger.getType()));
            }
        }

        List<WorkflowTask> enabledWorkflowTasks = WorkflowTaskUtils.getTasks(workflow.getTasks(), null);

        boolean writesToDataTable = enabledWorkflowTasks.stream()
            .map(WorkflowTask::getType)
            .anyMatch(type -> isOperation(type, DATA_TABLE, DATA_TABLE_WRITE_OPERATIONS));

        if (!writesToDataTable) {
            problems.add(NO_DATA_TABLE_WRITE_TASK);
        }

        return new CallableWorkflowCheck(problems, null);
    }

    @Override
    public Optional<String> fetchWorkflowUuid(String workflowId) {
        return projectWorkflowService.fetchWorkflowProjectWorkflow(workflowId)
            .map(ProjectWorkflow::getUuidAsString);
    }

    private static boolean isOperation(String type, String componentName, Set<String> operations) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        return Objects.equals(workflowNodeType.name(), componentName) &&
            operations.contains(workflowNodeType.operation());
    }
}
