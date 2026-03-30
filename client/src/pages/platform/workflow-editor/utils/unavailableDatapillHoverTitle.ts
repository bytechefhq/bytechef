import {ComponentDefinitionBasic, TaskDispatcherDefinition, Workflow} from '@/shared/middleware/platform/configuration';

import getAllTasksRecursively from './getAllTasksRecursively';

const GENERIC_UNAVAILABLE_DATAPILL_TITLE =
    'This data pill is not available from the current workflow. It may reference a removed node, a path that does not exist, or an output that is not in scope for this step.';

export function getWorkflowNodeNameFromDataPillValue(dataPillValue: string): string | undefined {
    const trimmedDataPillValue = dataPillValue.trim();

    if (!trimmedDataPillValue) {
        return undefined;
    }

    const withoutBrackets = trimmedDataPillValue.split('[')[0];

    const firstSegment = withoutBrackets.split('.')[0];

    return firstSegment || undefined;
}

interface GetComponentNameProps {
    type?: string;
    workflowNodeName: string;
}

function getComponentName({type, workflowNodeName}: GetComponentNameProps): string | undefined {
    if (type) {
        const segments = type.split('/');

        if (segments.length >= 2 && segments[0]) {
            return segments[0];
        }
    }

    const underscoreIndex = workflowNodeName.lastIndexOf('_');

    if (underscoreIndex <= 0) {
        return undefined;
    }

    const nodeNumberSuffix = workflowNodeName.slice(underscoreIndex + 1);

    if (!/^\d+$/.test(nodeNumberSuffix)) {
        return undefined;
    }

    return workflowNodeName.slice(0, underscoreIndex);
}

interface ResolveComponentTitleProps {
    componentDefinitions: Array<ComponentDefinitionBasic>;
    componentName: string;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
}

function resolveComponentTitle({
    componentDefinitions,
    componentName,
    taskDispatcherDefinitions,
}: ResolveComponentTitleProps): string {
    const dispatcherDefinition = taskDispatcherDefinitions.find((definition) => definition.name === componentName);

    if (dispatcherDefinition?.title) {
        return dispatcherDefinition.title;
    }

    const componentDefinition = componentDefinitions.find((definition) => definition.name === componentName);

    if (componentDefinition?.title) {
        return componentDefinition.title;
    }

    return componentName;
}

interface BuildUnavailableDataPillHoverTitleProps {
    componentDefinitions: Array<ComponentDefinitionBasic>;
    mentionId: string;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
    workflow: Workflow;
}

export function buildUnavailableDataPillHoverTitle({
    componentDefinitions,
    mentionId,
    taskDispatcherDefinitions,
    workflow,
}: BuildUnavailableDataPillHoverTitleProps): string {
    const nodeName = getWorkflowNodeNameFromDataPillValue(mentionId);

    if (!nodeName) {
        return GENERIC_UNAVAILABLE_DATAPILL_TITLE;
    }

    const allTasks = getAllTasksRecursively(workflow.tasks ?? []);

    const taskMatch = allTasks.find((task) => task.name === nodeName);
    const triggerMatch = workflow.triggers?.find((trigger) => trigger.name === nodeName);

    if (!taskMatch && !triggerMatch) {
        const componentName = getComponentName({workflowNodeName: nodeName});

        const resolvedTitle =
            componentName &&
            resolveComponentTitle({
                componentDefinitions,
                componentName,
                taskDispatcherDefinitions,
            });

        if (resolvedTitle && componentName && resolvedTitle !== componentName) {
            return `"${resolvedTitle}" is missing from the workflow. Re-add it before this step, or remove this dynamic value.`;
        }

        if (componentName) {
            return `"${resolvedTitle ?? componentName}" is missing from the workflow. Re-add it before this step, or remove this dynamic value.`;
        }

        return `"${nodeName}" node is missing from the workflow. Remove this dynamic value or restore that node.`;
    }

    const componentName = getComponentName({type: taskMatch?.type ?? triggerMatch?.type, workflowNodeName: nodeName});

    if (componentName) {
        const componentTitle = resolveComponentTitle({
            componentDefinitions,
            componentName,
            taskDispatcherDefinitions,
        });

        if (componentTitle) {
            return `From "${componentTitle}" component. This output is from a different action or the path is invalid - check order and branching, or remove this dynamic value.`;
        }
    }

    return `Output from this dynamic value is not available here. Check order, branching, and the property path, or remove this dynamic value.`;
}
