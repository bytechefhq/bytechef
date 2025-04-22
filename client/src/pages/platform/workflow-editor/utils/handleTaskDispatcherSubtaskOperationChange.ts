import {TASK_DISPATCHER_DATA_KEY_MAP} from '@/shared/constants';
import {ComponentDefinition, Workflow, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {PropertyAllType, TaskDispatcherContextType} from '@/shared/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import getRecursivelyUpdatedTasks from './getRecursivelyUpdatedTasks';
import saveWorkflowDefinition from './saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

interface HandleTaskDispatcherSubtaskOperationChangeProps {
    currentComponentDefinition: ComponentDefinition;
    currentNodeIndex: number;
    currentOperationProperties: Array<PropertyAllType>;
    newOperationName: string;
    projectId: string | number;
    queryClient: QueryClient;
    updateWorkflowMutation: UseMutationResult<void, Error, {id: string; workflow: Workflow}, unknown>;
}

export default function handleTaskDispatcherSubtaskOperationChange({
    currentComponentDefinition,
    currentNodeIndex,
    currentOperationProperties,
    newOperationName,
    projectId,
    queryClient,
    updateWorkflowMutation,
}: HandleTaskDispatcherSubtaskOperationChangeProps): void {
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
    let taskDispatcherComponentName: 'branch' | 'condition' | 'loop' | undefined = undefined;

    switch (taskDispatcherDataKey) {
        case 'branchData': {
            if (!currentNode.branchData) {
                console.error('No branch data found in current node');

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
                console.error('No condition data found in current node');

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
        case 'loopData': {
            if (!currentNode.loopData) {
                console.error('No loop data found in current node');

                break;
            }

            taskDispatcherContext = {
                index: currentNodeIndex,
                taskDispatcherId: currentNode.loopData.loopId,
            };

            taskDispatcherComponentName = 'loop';

            break;
        }
        default: {
            console.error('No task dispatcher data found');

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
        console.error(`No parent condition task found for ${taskDispatcherContext?.taskDispatcherId}`);

        return;
    }

    const subtasks: Array<WorkflowTask> = getSubtasks({
        context: taskDispatcherContext,
        task: parentTaskDispatcherTask,
    });

    const updatedSubtasks = subtasks.map((subtask) => {
        if (subtask.name === currentNode.name) {
            return {
                ...subtask,
                type: `${currentNode.componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
            };
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
        onSuccess: () => {
            const commonUpdates = {
                componentName,
                displayConditions: {},
                metadata: {},
                operationName: newOperationName,
                parameters: getParametersWithDefaultValues({
                    properties: currentOperationProperties as Array<PropertyAllType>,
                }),
                type: `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
                workflowNodeName,
            };

            setCurrentComponent({
                ...currentComponent,
                ...commonUpdates,
            });

            setCurrentNode({
                ...currentNode,
                ...commonUpdates,
                name: workflowNodeName || '',
            });
        },
        projectId: +projectId!,
        queryClient,
        taskDispatcherContext,
        updateWorkflowMutation,
        updatedWorkflowTasks: recursivelyUpdatedTasks,
    });
}
