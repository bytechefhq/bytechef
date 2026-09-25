import {WorkflowInput, WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';

import {WorkflowIssueI, getWorkflowIssueKey} from '../stores/useWorkflowIssuesStore';
import collectWorkflowIssues from './collectWorkflowIssues';
import findWorkflowIssueParameterPaths from './findWorkflowIssueParameterPaths';
import {forEachNestedTaskGroup} from './taskTraversalUtils';

type CodeEditorNodeIssueType = Pick<WorkflowIssueI, 'kind' | 'message' | 'nodeName' | 'severity'> & {
    propertyPath?: string | null;
};

interface WorkflowDefinitionI {
    inputs?: Array<Pick<WorkflowInput, 'name'>>;
    tasks?: Array<WorkflowTask>;
    triggers?: Array<WorkflowTrigger>;
}

interface GetWorkflowCodeEditorIssueMessagesProps {
    definition: string;
    errors: Array<string>;
    nodeIssues: Array<CodeEditorNodeIssueType>;
    warnings: Array<string>;
}

interface NamedParametersI {
    clusterElements?: unknown;
    name?: string;
    parameters?: Record<string, unknown>;
}

function collectClusterElementParameters(
    clusterElements: unknown,
    parametersByNodeName: Map<string, Record<string, unknown>>
): void {
    if (!clusterElements || typeof clusterElements !== 'object') {
        return;
    }

    for (const clusterElementValue of Object.values(clusterElements)) {
        const clusterElementItems: Array<NamedParametersI> = Array.isArray(clusterElementValue)
            ? clusterElementValue
            : [clusterElementValue];

        for (const clusterElementItem of clusterElementItems) {
            if (!clusterElementItem || typeof clusterElementItem !== 'object' || !clusterElementItem.name) {
                continue;
            }

            if (!parametersByNodeName.has(clusterElementItem.name)) {
                parametersByNodeName.set(clusterElementItem.name, clusterElementItem.parameters ?? {});
            }

            collectClusterElementParameters(clusterElementItem.clusterElements, parametersByNodeName);
        }
    }
}

function collectTaskParameters(
    tasks: Array<WorkflowTask>,
    parametersByNodeName: Map<string, Record<string, unknown>>
): void {
    for (const currentTask of tasks) {
        if (!parametersByNodeName.has(currentTask.name)) {
            parametersByNodeName.set(currentTask.name, (currentTask.parameters as Record<string, unknown>) ?? {});
        }

        collectClusterElementParameters(currentTask.clusterElements, parametersByNodeName);

        if (currentTask.parameters) {
            forEachNestedTaskGroup(currentTask.parameters as Record<string, unknown>, (nestedTasks) =>
                collectTaskParameters(nestedTasks, parametersByNodeName)
            );
        }
    }
}

function parseWorkflowDefinition(definition: string): WorkflowDefinitionI | undefined {
    try {
        const workflowDefinition = JSON.parse(definition);

        return workflowDefinition && typeof workflowDefinition === 'object' && !Array.isArray(workflowDefinition)
            ? workflowDefinition
            : undefined;
    } catch {
        return undefined;
    }
}

export default function getWorkflowCodeEditorIssueMessages({
    definition,
    errors,
    nodeIssues: serverNodeIssues,
    warnings,
}: GetWorkflowCodeEditorIssueMessagesProps): {errors: Array<string>; warnings: Array<string>} {
    const workflowDefinition = parseWorkflowDefinition(definition);

    const nodeIssues = serverNodeIssues.map((nodeIssue) => ({
        ...nodeIssue,
        propertyPath: nodeIssue.propertyPath ?? undefined,
    }));

    if (!workflowDefinition) {
        return {errors, warnings};
    }

    const tasks = Array.isArray(workflowDefinition.tasks) ? workflowDefinition.tasks : [];
    const triggers = Array.isArray(workflowDefinition.triggers) ? workflowDefinition.triggers : [];

    const parametersByNodeName = new Map<string, Record<string, unknown>>();

    for (const trigger of triggers) {
        parametersByNodeName.set(trigger.name, (trigger.parameters as Record<string, unknown>) ?? {});
    }

    collectTaskParameters(tasks, parametersByNodeName);

    const formatIssue = (
        issue: Pick<WorkflowIssueI, 'message' | 'nodeName' | 'propertyPath' | 'referencedNodeName'>
    ) => {
        const parameterPaths = findWorkflowIssueParameterPaths(issue, parametersByNodeName.get(issue.nodeName));

        return `[${issue.nodeName}] ${parameterPaths.length ? `${parameterPaths.join(', ')}: ` : ''}${issue.message}`;
    };

    const formattedMessagesByServerMessage = new Map(
        nodeIssues.map((nodeIssue) => [`[${nodeIssue.nodeName}] ${nodeIssue.message}`, formatIssue(nodeIssue)])
    );

    const errorMessages = new Set(errors.map((error) => formattedMessagesByServerMessage.get(error) ?? error));
    const warningMessages = new Set(
        warnings.map((warning) => formattedMessagesByServerMessage.get(warning) ?? warning)
    );

    const validatorIssueKeys = new Set(nodeIssues.map((nodeIssue) => getWorkflowIssueKey(nodeIssue)));

    const inputs = Array.isArray(workflowDefinition.inputs) ? workflowDefinition.inputs : [];

    for (const sweepIssue of collectWorkflowIssues({inputs, tasks, triggers})) {
        if (validatorIssueKeys.has(getWorkflowIssueKey(sweepIssue))) {
            continue;
        }

        (sweepIssue.severity === 'ERROR' ? errorMessages : warningMessages).add(formatIssue(sweepIssue));
    }

    return {errors: [...errorMessages], warnings: [...warningMessages]};
}
