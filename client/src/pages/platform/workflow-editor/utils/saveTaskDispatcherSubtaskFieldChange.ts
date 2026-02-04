import {TASK_DISPATCHER_DATA_KEY_MAP} from '@/shared/constants';
import {ComponentDefinition, Workflow, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType, PropertyAllType, TaskDispatcherContextType} from '@/shared/types';
import {UseMutationResult} from '@tanstack/react-query';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import getRecursivelyUpdatedTasks from './getRecursivelyUpdatedTasks';
import saveWorkflowDefinition from './saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

type FieldUpdateType = {
    field: 'operation' | 'label' | 'description';
    value: string;
};

interface SaveTaskDispatcherSubtaskFieldChangeProps {
    currentComponentDefinition: ComponentDefinition;
    currentNodeIndex: number;
    currentOperationProperties?: Array<PropertyAllType>;
    fieldUpdate: FieldUpdateType;
    invalidateWorkflowQueries: () => void;
    updateWorkflowMutation: UseMutationResult<void, Error, {id: string; workflow: Workflow}, unknown>;
}

export default function saveTaskDispatcherSubtaskFieldChange({
    currentComponentDefinition,
    currentNodeIndex,
    currentOperationProperties,
    fieldUpdate,
    invalidateWorkflowQueries,
    updateWorkflowMutation,
}: SaveTaskDispatcherSubtaskFieldChangeProps): void {
    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode} =
        useWorkflowNodeDetailsPanelStore.getState();

    const {workflow} = useWorkflowDataStore.getState();

    if (!currentNode || !workflow.definition) {
        return;
    }

    const {componentName, workflowNodeName} = currentNode;

    const taskDispatcherDataKey = Object.values(TASK_DISPATCHER_DATA_KEY_MAP).find(
        (dataKey) => dataKey && dataKey in currentNode && currentNode[dataKey as keyof typeof currentNode]
    );

    let taskDispatcherContext: TaskDispatcherContextType | undefined = undefined;
    let taskDispatcherComponentName: 'branch' | 'condition' | 'each' | 'fork-join' | 'loop' | 'parallel' | undefined =
        undefined;

    switch (taskDispatcherDataKey) {
        case 'branchData': {
            if (!currentNode.branchData) {
                break;
            }

            taskDispatcherContext = {
                caseKey: currentNode.branchData.caseKey,
                index: currentNodeIndex,
                taskDispatcherId: currentNode.branchData.branchId,
            };

            taskDispatcherComponentName = 'branch';

            break;
        }
        case 'conditionData': {
            if (!currentNode.conditionData) {
                break;
            }

            taskDispatcherContext = {
                conditionCase: currentNode.conditionData.conditionCase,
                index: currentNodeIndex,
                taskDispatcherId: currentNode.conditionData.conditionId,
            };

            taskDispatcherComponentName = 'condition';

            break;
        }
        case 'eachData': {
            if (!currentNode.eachData) {
                break;
            }

            taskDispatcherContext = {
                index: currentNodeIndex,
                taskDispatcherId: currentNode.eachData.eachId,
            };

            taskDispatcherComponentName = 'each';

            break;
        }
        case 'loopData': {
            if (!currentNode.loopData) {
                break;
            }

            taskDispatcherContext = {
                index: currentNodeIndex,
                taskDispatcherId: currentNode.loopData.loopId,
            };

            taskDispatcherComponentName = 'loop';

            break;
        }
        case 'parallelData': {
            if (!currentNode.parallelData) {
                break;
            }

            taskDispatcherContext = {
                index: currentNodeIndex,
                taskDispatcherId: currentNode.parallelData.parallelId,
            };

            taskDispatcherComponentName = 'parallel';

            break;
        }
        case 'forkJoinData': {
            if (!currentNode.forkJoinData) {
                break;
            }

            taskDispatcherContext = {
                branchIndex: currentNode.forkJoinData.branchIndex,
                index: currentNodeIndex,
                taskDispatcherId: currentNode.forkJoinData.forkJoinId,
            };

            taskDispatcherComponentName = 'fork-join';

            break;
        }
        default: {
            return;
        }
    }

    if (!taskDispatcherContext || !taskDispatcherComponentName) {
        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

    const {getSubtasks, getTask, updateTaskParameters} = TASK_DISPATCHER_CONFIG[taskDispatcherComponentName];

    const parentTaskDispatcherTask = getTask({
        taskDispatcherId: taskDispatcherContext?.taskDispatcherId,
        tasks: workflowDefinitionTasks,
    });

    if (!parentTaskDispatcherTask) {
        console.error(`No parent task dispatcher found for ${taskDispatcherContext?.taskDispatcherId}`);

        return;
    }

    const subtasks: Array<WorkflowTask> = getSubtasks({
        context: taskDispatcherContext,
        task: parentTaskDispatcherTask,
    });

    const updatedSubtasks = subtasks.map((subtask) => {
        if (subtask.name === currentNode.name) {
            switch (fieldUpdate.field) {
                case 'operation':
                    return {
                        ...subtask,
                        parameters: getParametersWithDefaultValues({
                            properties: currentOperationProperties as Array<PropertyAllType>,
                        }),
                        type: `${currentNode.componentName}/v${currentComponentDefinition.version}/${fieldUpdate.value}`,
                    };
                case 'label':
                    return {
                        ...subtask,
                        label: fieldUpdate.value,
                    };
                case 'description':
                    return {
                        ...subtask,
                        description: fieldUpdate.value,
                    };
                default:
                    return subtask;
            }
        }

        return subtask;
    });

    const updatedTaskDispatcherTask = updateTaskParameters({
        context: taskDispatcherContext,
        task: parentTaskDispatcherTask,
        updatedSubtasks,
    });

    const recursivelyUpdatedTasks = getRecursivelyUpdatedTasks(workflowDefinitionTasks, updatedTaskDispatcherTask);

    saveWorkflowDefinition({
        invalidateWorkflowQueries,
        onSuccess: () => {
            let commonUpdates: NodeDataType = {
                componentName,
                name: currentNode.name,
                workflowNodeName,
            };

            if (fieldUpdate.field === 'operation') {
                commonUpdates = {
                    ...commonUpdates,
                    displayConditions: {},
                    metadata: {},
                    operationName: fieldUpdate.value,
                    parameters: getParametersWithDefaultValues({
                        properties: currentOperationProperties as Array<PropertyAllType>,
                    }),
                    type: `${componentName}/v${currentComponentDefinition.version}/${fieldUpdate.value}`,
                };
            } else {
                commonUpdates[fieldUpdate.field] = fieldUpdate.value;
            }

            setCurrentComponent({
                ...currentComponent,
                ...commonUpdates,
            });

            setCurrentNode({
                ...currentNode,
                ...commonUpdates,
                name: workflowNodeName || '',
            });

            useWorkflowNodeDetailsPanelStore.getState().setOperationChangeInProgress(false);
        },
        taskDispatcherContext,
        updateWorkflowMutation,
        updatedWorkflowTasks: recursivelyUpdatedTasks,
    });
}
