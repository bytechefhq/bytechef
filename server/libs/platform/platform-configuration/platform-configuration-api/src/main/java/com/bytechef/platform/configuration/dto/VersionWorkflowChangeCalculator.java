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

package com.bytechef.platform.configuration.dto;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.configuration.service.WorkflowDefinitionDiffService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class VersionWorkflowChangeCalculator {

    private static final Comparator<VersionWorkflowChangeSummary> SUMMARY_COMPARATOR = Comparator
        .comparingInt((VersionWorkflowChangeSummary summary) -> changeTypeOrder(summary.changeType()))
        .thenComparing(
            VersionWorkflowChangeSummary::label, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private VersionWorkflowChangeCalculator() {
    }

    public static Optional<VersionWorkflowChange> fetchChange(
        @Nullable Integer baseVersion, int version, String workflowUuid, Map<String, Workflow> oldWorkflowsByUuid,
        Map<String, Workflow> newWorkflowsByUuid, WorkflowDefinitionDiffService workflowDefinitionDiffService) {

        Workflow oldWorkflow = oldWorkflowsByUuid.get(workflowUuid);
        Workflow newWorkflow = newWorkflowsByUuid.get(workflowUuid);

        if (oldWorkflow == null && newWorkflow == null) {
            return Optional.empty();
        }

        WorkflowDefinitionDiff diff = workflowDefinitionDiffService.diff(oldWorkflow, newWorkflow);

        return Optional.of(
            VersionWorkflowChangeFactory.change(workflowUuid, baseVersion, version, oldWorkflow, newWorkflow, diff));
    }

    public static @Nullable Integer resolveBaseVersion(Collection<Integer> versions, int version) {
        Integer baseVersion = null;

        for (Integer existingVersion : versions) {
            if (existingVersion < version && (baseVersion == null || existingVersion > baseVersion)) {
                baseVersion = existingVersion;
            }
        }

        return baseVersion;
    }

    public static VersionWorkflowChanges summarize(
        @Nullable Integer baseVersion, int version, Map<String, Workflow> oldWorkflowsByUuid,
        Map<String, Workflow> newWorkflowsByUuid, WorkflowDefinitionDiffService workflowDefinitionDiffService) {

        Set<String> workflowUuids = new LinkedHashSet<>(oldWorkflowsByUuid.keySet());

        workflowUuids.addAll(newWorkflowsByUuid.keySet());

        List<VersionWorkflowChangeSummary> summaries = new ArrayList<>();

        for (String workflowUuid : workflowUuids) {
            Workflow oldWorkflow = oldWorkflowsByUuid.get(workflowUuid);
            Workflow newWorkflow = newWorkflowsByUuid.get(workflowUuid);

            WorkflowDefinitionDiff diff = workflowDefinitionDiffService.diff(oldWorkflow, newWorkflow);

            if (diff.changeType() != WorkflowChangeType.UNCHANGED) {
                summaries.add(VersionWorkflowChangeFactory.summary(workflowUuid, oldWorkflow, newWorkflow, diff));
            }
        }

        summaries.sort(SUMMARY_COMPARATOR);

        return new VersionWorkflowChanges(baseVersion, version, summaries);
    }

    private static int changeTypeOrder(WorkflowChangeType changeType) {
        return switch (changeType) {
            case ADDED -> 0;
            case MODIFIED -> 1;
            case REMOVED -> 2;
            case UNCHANGED -> 3;
        };
    }
}
