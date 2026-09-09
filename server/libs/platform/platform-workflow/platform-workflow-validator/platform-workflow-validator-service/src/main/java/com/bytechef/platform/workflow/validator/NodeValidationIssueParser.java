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

package com.bytechef.platform.workflow.validator;

import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.NodeValidationIssue;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueKind;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueSeverity;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
class NodeValidationIssueParser {

    private static final Pattern TASK_PREFIX_PATTERN = Pattern.compile("^\\[([^\\]]+)] (.*)$", Pattern.DOTALL);
    private static final Pattern DUPLICATE_NODE_NAME_PATTERN = Pattern.compile(
        "^Node names must be unique\\. Duplicate node name: (.+)$");
    private static final List<MessageTemplate> MESSAGE_TEMPLATES = List.of(
        new MessageTemplate(Pattern.compile("^Missing required property: (.+)$"), WorkflowIssueKind.MISSING_REQUIRED),
        new MessageTemplate(Pattern.compile("^Missing required field: (.+)$"), WorkflowIssueKind.MISSING_REQUIRED),
        new MessageTemplate(
            Pattern.compile("^Resource referenced by property '([^']+)' is not available: .*$", Pattern.DOTALL),
            WorkflowIssueKind.MISSING_RESOURCE),
        new MessageTemplate(
            Pattern.compile("^Could not verify the resource referenced by property '([^']+)': .*$", Pattern.DOTALL),
            WorkflowIssueKind.MISSING_RESOURCE),
        new MessageTemplate(
            Pattern.compile("^Wrong task order: You can't reference '([^']+)' .*$"), WorkflowIssueKind.TASK_ORDER),
        new MessageTemplate(
            Pattern.compile("^Property '([^']+)' does not exist in the output of .*$"),
            WorkflowIssueKind.BROKEN_REFERENCE),
        new MessageTemplate(
            Pattern.compile("^Property '([^']+)' might not exist in the output of .*$"),
            WorkflowIssueKind.BROKEN_REFERENCE),
        new MessageTemplate(
            Pattern.compile("^Property '([^']+)' has incorrect type\\..*$"), WorkflowIssueKind.TYPE_MISMATCH),
        new MessageTemplate(
            Pattern.compile("^Property '([^']+)' in output of '[^']+' is of type .*$"),
            WorkflowIssueKind.TYPE_MISMATCH),
        new MessageTemplate(
            Pattern.compile("^Value .* has incorrect type in property '([^']+)'\\..*$"),
            WorkflowIssueKind.TYPE_MISMATCH),
        new MessageTemplate(
            Pattern.compile("^Cluster element '([^']+)' .*$"), WorkflowIssueKind.MISSING_CLUSTER_ELEMENT),
        new MessageTemplate(
            Pattern.compile("^Property '([^']+)' is not defined in task definition$"), WorkflowIssueKind.OTHER),
        new MessageTemplate(Pattern.compile("^Field '([^']+)' must .*$"), WorkflowIssueKind.TYPE_MISMATCH),
        new MessageTemplate(
            Pattern.compile("^Missing recommended field: (.+)$"), WorkflowIssueKind.MISSING_REQUIRED),
        new MessageTemplate(
            Pattern.compile("^Task '([^']+)' doesn't exist\\.$"), WorkflowIssueKind.BROKEN_REFERENCE),
        new MessageTemplate(
            Pattern.compile("^Input property '([^']+)' is of type .*$"), WorkflowIssueKind.TYPE_MISMATCH),
        new MessageTemplate(
            Pattern.compile("^Property '([^']+)' is in incorrect .*$"), WorkflowIssueKind.TYPE_MISMATCH),
        new MessageTemplate(
            Pattern.compile("^Property '([^']+)' does not match any of the expected union types: .*$"),
            WorkflowIssueKind.TYPE_MISMATCH),
        new MessageTemplate(
            Pattern.compile("^Invalid logic for display condition: (.+)$"), WorkflowIssueKind.OTHER));

    private NodeValidationIssueParser() {
    }

    static List<NodeValidationIssue> parse(List<String> errors, List<String> warnings) {
        List<NodeValidationIssue> nodeValidationIssues = new ArrayList<>();

        for (String error : errors) {
            NodeValidationIssue nodeValidationIssue = toIssue(error, WorkflowIssueSeverity.ERROR);

            if (nodeValidationIssue != null) {
                nodeValidationIssues.add(nodeValidationIssue);
            }
        }

        for (String warning : warnings) {
            NodeValidationIssue nodeValidationIssue = toIssue(warning, WorkflowIssueSeverity.WARNING);

            if (nodeValidationIssue != null) {
                nodeValidationIssues.add(nodeValidationIssue);
            }
        }

        return nodeValidationIssues;
    }

    @Nullable
    private static NodeValidationIssue toIssue(String line, WorkflowIssueSeverity severity) {
        Matcher prefixMatcher = TASK_PREFIX_PATTERN.matcher(line);

        if (prefixMatcher.matches()) {
            return classify(prefixMatcher.group(1), prefixMatcher.group(2), severity);
        }

        Matcher duplicateMatcher = DUPLICATE_NODE_NAME_PATTERN.matcher(line);

        if (duplicateMatcher.matches()) {
            return new NodeValidationIssue(
                duplicateMatcher.group(1), null, WorkflowIssueKind.DUPLICATE_NODE_NAME, severity, line);
        }

        return null;
    }

    private static NodeValidationIssue classify(String nodeName, String message, WorkflowIssueSeverity severity) {
        for (MessageTemplate messageTemplate : MESSAGE_TEMPLATES) {
            Matcher matcher = messageTemplate.pattern()
                .matcher(message);

            if (matcher.matches()) {
                return new NodeValidationIssue(nodeName, matcher.group(1), messageTemplate.kind(), severity, message);
            }
        }

        return new NodeValidationIssue(nodeName, null, WorkflowIssueKind.OTHER, severity, message);
    }

    private record MessageTemplate(Pattern pattern, WorkflowIssueKind kind) {
    }
}
