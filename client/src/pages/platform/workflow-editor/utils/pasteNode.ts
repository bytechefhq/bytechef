import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import getFormattedName from '@/pages/platform/workflow-editor/utils/getFormattedName';
import saveWorkflowDefinition from '@/pages/platform/workflow-editor/utils/saveWorkflowDefinition';
import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {
    BranchCaseType,
    ClusterElementItemType,
    ClusterElementsType,
    NodeDataType,
    TaskDispatcherContextType,
    UpdateWorkflowMutationType,
} from '@/shared/types';

type TaskParametersType = NonNullable<WorkflowTask['parameters']>;

interface PasteNodeI {
    cancelWorkflowQueries?: () => void;
    nodeIndex?: number;
    sourceNodeName?: string;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const isTaskDispatcher = (componentName: string): boolean => TASK_DISPATCHER_NAMES.includes(componentName);

const getNextAvailableName = (copiedLabel: string, existingLabels: string[]): string => {
    let counter = 1;

    const escapedCopiedLabel = copiedLabel.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

    const labelWithCounterRegex = new RegExp(`^${escapedCopiedLabel} \\((\\d+)\\)$`);

    existingLabels.forEach((label) => {
        const matchingLabel = label.match(labelWithCounterRegex);

        if (matchingLabel) {
            const number = parseInt(matchingLabel[1], 10);

            if (number >= counter) {
                counter = number + 1;
            }
        }
    });

    return `${copiedLabel} (${counter})`;
};

function recursivelyStripEmptyValuesFromParameters(parameters: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = {};

    for (const [key, value] of Object.entries(parameters)) {
        if (value === null || value === undefined || value === '') {
            continue;
        }

        if (Array.isArray(value)) {
            if (value.length > 0) {
                result[key] = value;
            }
        } else if (typeof value === 'object') {
            const stripped = recursivelyStripEmptyValuesFromParameters(value as Record<string, unknown>);

            if (Object.keys(stripped).length > 0) {
                result[key] = stripped;
            }
        } else {
            result[key] = value;
        }
    }

    return result;
}

function renameClusterElements(clusterElements: ClusterElementsType, reservedNames: Set<string>): ClusterElementsType {
    const renameElement = (element: ClusterElementItemType): ClusterElementItemType => {
        const elementComponentName = element.type?.split('/')?.[0];

        if (!elementComponentName) {
            return element;
        }

        const newName = getFormattedName(elementComponentName, reservedNames);

        reservedNames.add(newName);

        const updatedElement: ClusterElementItemType = {
            ...element,
            metadata: element.metadata
                ? {...element.metadata, ui: {...element.metadata.ui, nodePosition: undefined}}
                : undefined,
            name: newName,
        };

        if (updatedElement.clusterElements) {
            updatedElement.clusterElements = renameClusterElements(
                updatedElement.clusterElements as ClusterElementsType,
                reservedNames
            );
        }

        return updatedElement;
    };

    const result: ClusterElementsType = {};

    for (const [elementType, elementValue] of Object.entries(clusterElements)) {
        if (elementValue === null) {
            result[elementType] = null;
        } else if (Array.isArray(elementValue)) {
            result[elementType] = elementValue.map(renameElement);
        } else {
            result[elementType] = renameElement(elementValue);
        }
    }

    return result;
}

function renameNestedTasks({
    componentName,
    parameters,
    reservedNames,
}: {
    parameters: TaskParametersType;
    componentName: string;
    reservedNames: Set<string>;
}): TaskParametersType {
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
            task.parameters = renameNestedTasks({
                componentName: subtaskComponentName,
                parameters: task.parameters,
                reservedNames,
            });
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
        default:
            break;
    }

    return parameters;
}

export default function pasteNode({
    cancelWorkflowQueries,
    nodeIndex: directNodeIndex,
    sourceNodeName,
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

    if (resolvedNodeIndex === undefined && sourceNodeName) {
        if (!taskDispatcherContext?.taskDispatcherId) {
            let sourceIndex = definitionTasks.findIndex((task) => task.name === sourceNodeName);

            if (sourceIndex === -1) {
                const sourceNode = nodes.find((node) => node.id === sourceNodeName);

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

    const reservedNames = new Set<string>(definitionTasks.map((task: {name: string}) => task.name));

    const newName = getFormattedName(copiedNode.componentName, reservedNames);

    reservedNames.add(newName);

    const clonedParameters = structuredClone(copiedNode.parameters ?? {});
    const workflowDefinitionTask = definitionTasks.find(
        (task: {name: string}) => task.name === (copiedNode.workflowNodeName ?? copiedNode.name)
    ) as {clusterElements?: ClusterElementsType} | undefined;

    const clonedClusterElements = workflowDefinitionTask?.clusterElements
        ? structuredClone(workflowDefinitionTask.clusterElements)
        : copiedNode.clusterElements && !Array.isArray(copiedNode.clusterElements)
          ? structuredClone(copiedNode.clusterElements)
          : undefined;

    const shouldResetLayout = nodes.some(
        (node) => (node.data as NodeDataType)?.metadata?.ui?.nodePosition !== undefined
    );

    if (isTaskDispatcher(copiedNode.componentName)) {
        renameNestedTasks({
            componentName: copiedNode.componentName,
            parameters: clonedParameters,
            reservedNames,
        });
    }

    const renamedClusterElements = clonedClusterElements
        ? renameClusterElements(clonedClusterElements, reservedNames)
        : undefined;

    const cleanedParameters = recursivelyStripEmptyValuesFromParameters(clonedParameters);

    const existingLabels = nodes.map((node) => node.data?.label).filter((label): label is string => !!label);

    const finalLabel = existingLabels.includes(copiedNode.label ?? '')
        ? getNextAvailableName(copiedNode.label ?? '', existingLabels)
        : (copiedNode.label ?? '');

    let cleanedMetadata = undefined;

    if (copiedNode.metadata) {
        const {ui, ...restMetadata} = copiedNode.metadata;

        const restUi = Object.fromEntries(Object.entries(ui || {}).filter(([key]) => key !== 'nodePosition'));

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
        clusterElements: renamedClusterElements ?? copiedNode.clusterElements,
        label: finalLabel,
        metadata: cleanedMetadata,
        name: newName,
        parameters: cleanedParameters,
        workflowNodeName: finalLabel,
    };

    if (shouldResetLayout) {
        useWorkflowEditorStore.getState().setResetWorkflowLayout(true);
    }

    if (cancelWorkflowQueries) {
        cancelWorkflowQueries();
    }

    saveWorkflowDefinition({
        nodeData: newNodeData,
        nodeIndex: resolvedNodeIndex,
        taskDispatcherContext,
        updateWorkflowMutation,
    });
}
