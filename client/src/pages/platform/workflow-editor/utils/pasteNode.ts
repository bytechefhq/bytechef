import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import getFormattedName from '@/pages/platform/workflow-editor/utils/getFormattedName';
import saveWorkflowDefinition from '@/pages/platform/workflow-editor/utils/saveWorkflowDefinition';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {TaskDispatcherContextType, UpdateWorkflowMutationType} from '@/shared/types';

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

function renameNestedTasks(parameters: TaskParametersType, componentName: string): TaskParametersType {
    const renameTask = (task: WorkflowTask) => {
        const subtaskComponentName = task.type?.split('/')?.[0];

        if (!subtaskComponentName) {
            return;
        }

        task.name = getFormattedName(subtaskComponentName);

        if (isTaskDispatcher(subtaskComponentName) && task.parameters) {
            task.parameters = renameNestedTasks(task.parameters, subtaskComponentName);
        }
    };

    switch (componentName) {
        case 'condition': {
            if (Array.isArray(parameters.caseTrue)) {
                for (const task of parameters.caseTrue) {
                    renameTask(task);
                }
            }

            if (Array.isArray(parameters.caseFalse)) {
                for (const task of parameters.caseFalse) {
                    renameTask(task);
                }
            }

            break;
        }

        case 'branch': {
            if (Array.isArray(parameters.cases)) {
                for (const caseItem of parameters.cases) {
                    if (Array.isArray(caseItem.tasks)) {
                        for (const task of caseItem.tasks) {
                            renameTask(task);
                        }
                    }
                }
            }

            if (Array.isArray(parameters.default)) {
                for (const task of parameters.default) {
                    renameTask(task);
                }
            }

            break;
        }

        case 'loop':
        case 'map': {
            if (Array.isArray(parameters.iteratee)) {
                for (const task of parameters.iteratee) {
                    renameTask(task);
                }
            }

            break;
        }

        case 'each': {
            if (parameters.iteratee && typeof parameters.iteratee === 'object' && !Array.isArray(parameters.iteratee)) {
                renameTask(parameters.iteratee);
            }

            break;
        }

        case 'parallel': {
            if (Array.isArray(parameters.tasks)) {
                for (const task of parameters.tasks) {
                    renameTask(task);
                }
            }

            break;
        }

        case 'fork-join': {
            if (Array.isArray(parameters.branches)) {
                for (const branch of parameters.branches) {
                    if (Array.isArray(branch)) {
                        for (const task of branch) {
                            renameTask(task);
                        }
                    }
                }
            }

            break;
        }

        case 'on-error': {
            if (Array.isArray(parameters['main-branch'])) {
                for (const task of parameters['main-branch']) {
                    renameTask(task);
                }
            }

            if (Array.isArray(parameters['on-error-branch'])) {
                for (const task of parameters['on-error-branch']) {
                    renameTask(task);
                }
            }

            break;
        }

        default:
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
    const copiedNode = useWorkflowEditorStore.getState().copiedNode;

    if (!copiedNode) {
        return;
    }

    const {workflow} = useWorkflowDataStore.getState();

    let definitionTasks: Array<{name: string}>;

    try {
        const workflowDefinition = JSON.parse(workflow.definition!);

        definitionTasks = workflowDefinition.tasks ?? [];
    } catch {
        return;
    }

    let nodeIndex: number | undefined = directNodeIndex;

    if (nodeIndex === undefined && nodeSourceName) {
        if (!taskDispatcherContext?.taskDispatcherId) {
            let sourceIndex = definitionTasks.findIndex((task) => task.name === nodeSourceName);

            if (sourceIndex === -1 && nodeSourceName) {
                const {nodes} = useWorkflowDataStore.getState();
                const sourceNode = nodes.find((node) => node.id === nodeSourceName);
                const resolvedName = sourceNode?.data?.taskDispatcherId as string | undefined;

                if (resolvedName) {
                    sourceIndex = definitionTasks.findIndex((task) => task.name === resolvedName);
                } else if (sourceNode?.data?.trigger) {
                    nodeIndex = 0;
                }
            }

            if (nodeIndex === undefined) {
                nodeIndex = sourceIndex !== -1 ? sourceIndex + 1 : undefined;
            }
        }
    }

    const newName = getFormattedName(copiedNode.componentName);

    const clonedParameters = structuredClone(copiedNode.parameters ?? {});

    if (isTaskDispatcher(copiedNode.componentName)) {
        renameNestedTasks(clonedParameters, copiedNode.componentName);
    }

    const newNodeData = {
        ...copiedNode,
        label: `copy of ${copiedNode.workflowNodeName}`,
        metadata: copiedNode.metadata?.ui?.nodePosition
            ? {
                  ...copiedNode.metadata,
                  ui: {
                      ...copiedNode.metadata.ui,
                      nodePosition: {
                          x: copiedNode.metadata.ui.nodePosition.x + 50,
                          y: copiedNode.metadata.ui.nodePosition.y + 50,
                      },
                  },
              }
            : copiedNode.metadata,
        name: newName,
        parameters: clonedParameters,
        workflowNodeName: newName,
    };

    saveWorkflowDefinition({
        nodeData: newNodeData,
        nodeIndex,
        taskDispatcherContext,
        updateWorkflowMutation,
    });
}
