import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import getFormattedName from '@/pages/platform/workflow-editor/utils/getFormattedName';
import saveWorkflowDefinition from '@/pages/platform/workflow-editor/utils/saveWorkflowDefinition';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType, NodeDataType, TaskDispatcherContextType, UpdateWorkflowMutationType} from '@/shared/types';

type TaskParametersType = NonNullable<WorkflowTask['parameters']>;

interface PasteNodeI {
    nodeIndex?: number;
    nodeSourceName?: string;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const TASK_DISPATCHER_NAMES = new Set([
    'condition',
    'branch',
    'loop',
    'map',
    'each',
    'parallel',
    'fork-join',
    'on-error',
]);

const isTaskDispatcher = (componentName: string): boolean => TASK_DISPATCHER_NAMES.has(componentName);

const getNextAvailableName = (copiedLabel: string, existingLabels: string[]): string => {
    let counter = 1;
    const escapedBase = copiedLabel.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const regex = new RegExp(`^${escapedBase} \\((\\d+)\\)$`);

    existingLabels.forEach((label) => {
        const match = label.match(regex);
        if (match) {
            const num = parseInt(match[1], 10);
            if (num >= counter) {
                counter = num + 1;
            }
        }
    });

    return `${copiedLabel} (${counter})`;
};

function stripEmptyValues(obj: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = {};

    for (const [key, value] of Object.entries(obj)) {
        if (value === null || value === undefined || value === '') {
            continue;
        }

        if (Array.isArray(value)) {
            if (value.length > 0) {
                result[key] = value;
            }
        } else if (typeof value === 'object') {
            const stripped = stripEmptyValues(value as Record<string, unknown>);

            if (Object.keys(stripped).length > 0) {
                result[key] = stripped;
            }
        } else {
            result[key] = value;
        }
    }

    return result;
}

function renameNestedTasks(
    parameters: TaskParametersType,
    componentName: string,
    reservedNames: Set<string>
): TaskParametersType {
    const renameTask = (task: WorkflowTask) => {
        const subtaskComponentName = task.type?.split('/')?.[0];

        if (!subtaskComponentName) {
            return;
        }

        if (task.metadata?.ui) {
            task.metadata.ui = {...task.metadata.ui, nodePosition: undefined};
        }

        const newName = getFormattedName(subtaskComponentName, reservedNames);
        reservedNames.add(newName);
        task.name = newName;

        if (isTaskDispatcher(subtaskComponentName) && task.parameters) {
            task.parameters = renameNestedTasks(task.parameters, subtaskComponentName, reservedNames);
        }
    };

    switch (componentName) {
        case 'condition':
            if (Array.isArray(parameters.caseTrue)) {
                (parameters.caseTrue as WorkflowTask[]).forEach(renameTask);
            }
            if (Array.isArray(parameters.caseFalse)) {
                (parameters.caseFalse as WorkflowTask[]).forEach(renameTask);
            }
            break;
        case 'branch':
            if (Array.isArray(parameters.cases)) {
                (parameters.cases as BranchCaseType[]).forEach((caseItem) => caseItem.tasks?.forEach(renameTask));
            }
            if (Array.isArray(parameters.default)) {
                (parameters.default as WorkflowTask[]).forEach(renameTask);
            }
            break;
        case 'loop':
        case 'map':
            if (Array.isArray(parameters.iteratee)) {
                (parameters.iteratee as WorkflowTask[]).forEach(renameTask);
            }
            break;
        case 'each':
            if (parameters.iteratee && typeof parameters.iteratee === 'object' && !Array.isArray(parameters.iteratee)) {
                renameTask(parameters.iteratee as WorkflowTask);
            }
            break;
        case 'parallel':
            if (Array.isArray(parameters.tasks)) {
                (parameters.tasks as WorkflowTask[]).forEach(renameTask);
            }
            break;
        case 'fork-join':
            if (Array.isArray(parameters.branches)) {
                (parameters.branches as WorkflowTask[][]).forEach((branch) => branch.forEach(renameTask));
            }
            break;
        case 'on-error':
            if (Array.isArray(parameters['main-branch'])) {
                (parameters['main-branch'] as WorkflowTask[]).forEach(renameTask);
            }
            if (Array.isArray(parameters['on-error-branch'])) {
                (parameters['on-error-branch'] as WorkflowTask[]).forEach(renameTask);
            }
            break;
    }

    return parameters;
}

export default function pasteNode({
    nodeIndex: directNodeIndex,
    nodeSourceName,
    taskDispatcherContext,
    updateWorkflowMutation,
}: PasteNodeI) {
    const {copiedNode, copiedWorkflowId} = useWorkflowEditorStore.getState();

    if (!copiedNode) {
        return;
    }

    const {nodes, workflow} = useWorkflowDataStore.getState();

    if (copiedWorkflowId !== workflow.id) {
        return;
    }

    if (!workflow.definition) {
        return;
    }

    let definitionTasks: Array<{name: string}>;

    try {
        const workflowDefinition = JSON.parse(workflow.definition);
        definitionTasks = workflowDefinition.tasks ?? [];
    } catch {
        return;
    }

    let resolvedNodeIndex: number | undefined = directNodeIndex;

    if (resolvedNodeIndex === undefined && nodeSourceName) {
        if (!taskDispatcherContext?.taskDispatcherId) {
            let sourceIndex = definitionTasks.findIndex((task) => task.name === nodeSourceName);

            if (sourceIndex === -1) {
                const sourceNode = nodes.find((node) => node.id === nodeSourceName);
                const resolvedName = sourceNode?.data?.taskDispatcherId as string | undefined;

                if (resolvedName) {
                    sourceIndex = definitionTasks.findIndex((task) => task.name === resolvedName);
                } else if (sourceNode?.data?.trigger) {
                    resolvedNodeIndex = 0;
                }
            }

            if (resolvedNodeIndex === undefined) {
                resolvedNodeIndex = sourceIndex !== -1 ? sourceIndex + 1 : undefined;
            }
        }
    }

    const reservedNames = new Set<string>();
    const newName = getFormattedName(copiedNode.componentName, reservedNames);
    reservedNames.add(newName);

    const clonedParameters = structuredClone(copiedNode.parameters ?? {});

    const shouldResetLayout = nodes.some(
        (node) => (node.data as NodeDataType)?.metadata?.ui?.nodePosition !== undefined
    );

    if (isTaskDispatcher(copiedNode.componentName)) {
        renameNestedTasks(clonedParameters, copiedNode.componentName, reservedNames);
    }

    const cleanedParameters = stripEmptyValues(clonedParameters);

    const existingLabels = nodes.map((node) => node.data?.label).filter((label): label is string => !!label);
    const finalLabel = getNextAvailableName(copiedNode.label ?? '', existingLabels);

    let cleanedMetadata = undefined;

    if (copiedNode.metadata) {
        const {ui, ...restMetadata} = copiedNode.metadata;

        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const {nodePosition, ...restUi} = ui || {};

        const cleanedUi = Object.keys(restUi).length > 0 ? restUi : undefined;

        if (Object.keys(restMetadata).length > 0 || cleanedUi) {
            cleanedMetadata = {
                ...restMetadata,
                ...(cleanedUi ? {ui: cleanedUi} : {}),
            };
        }
    }

    const newNodeData = {
        ...copiedNode,
        label: finalLabel,
        metadata: cleanedMetadata,
        name: newName,
        parameters: cleanedParameters,
        workflowNodeName: finalLabel,
    };

    if (shouldResetLayout) {
        useWorkflowEditorStore.getState().setResetWorkflowLayout(true);
    }

    saveWorkflowDefinition({
        nodeData: newNodeData,
        nodeIndex: resolvedNodeIndex,
        taskDispatcherContext,
        updateWorkflowMutation,
    });
}
