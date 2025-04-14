import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {SPACE} from '@/shared/constants';
import {Workflow, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {BranchCaseType, NodeDataType, WorkflowDefinitionType, WorkflowTaskType} from '@/shared/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getParentTaskDispatcherTask from './getParentTaskDispatcherTask';

interface HandleDeleteTaskProps {
    currentNode?: NodeDataType;
    data: NodeDataType;
    projectId: number;
    queryClient: QueryClient;
    updateWorkflowMutation: UseMutationResult<void, unknown, {id: string; workflow: Workflow}>;
    workflow: Workflow & WorkflowTaskDataType;
}

export default function handleDeleteTask({
    currentNode,
    data,
    projectId,
    queryClient,
    updateWorkflowMutation,
    workflow,
}: HandleDeleteTaskProps) {
    if (!workflow?.definition) {
        return;
    }

    const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition);

    const {tasks: workflowTasks} = workflowDefinition;

    if (!workflowTasks) {
        return;
    }

    let updatedTasks: Array<WorkflowTaskType>;

    if (data.conditionData) {
        const parentConditionTask = getParentTaskDispatcherTask({
            taskDispatcherId: data.conditionData.conditionId,
            tasks: workflowTasks,
        });

        const taskConditionCase = data.conditionData.conditionCase;

        if (!parentConditionTask?.parameters) {
            return;
        }

        parentConditionTask.parameters[taskConditionCase as string] = (
            parentConditionTask.parameters[taskConditionCase] as Array<WorkflowTask>
        ).filter((childTask) => childTask.name !== data.name);

        updatedTasks = workflowTasks.map((task) => {
            if (task.name !== parentConditionTask.name) {
                return task;
            }

            return parentConditionTask;
        }) as Array<WorkflowTaskType>;
    } else if (data.loopData) {
        const parentLoopTask = getParentTaskDispatcherTask({
            taskDispatcherId: data.loopData.loopId,
            tasks: workflowTasks,
        });

        if (!parentLoopTask?.parameters) {
            return;
        }

        parentLoopTask.parameters.iteratee = (parentLoopTask.parameters.iteratee as Array<WorkflowTask>).filter(
            (childTask) => childTask.name !== data.name
        );

        updatedTasks = workflowTasks.map((task) => {
            if (task.name !== parentLoopTask.name) {
                return task;
            }

            return parentLoopTask;
        }) as Array<WorkflowTaskType>;
    } else if (data.branchData) {
        const parentBranchTask = getParentTaskDispatcherTask({
            taskDispatcherId: data.branchData.branchId,
            tasks: workflowTasks,
        });

        if (!parentBranchTask?.parameters) {
            return;
        }

        const {caseKey} = data.branchData;
        const {name, parameters} = parentBranchTask;

        if (caseKey === 'default') {
            parentBranchTask.parameters.default = (parameters.default as Array<WorkflowTask>).filter(
                (childTask) => childTask.name !== data.name
            );
        } else if (parameters.cases) {
            parameters.cases = (parameters.cases as BranchCaseType[]).map((caseItem) => {
                if (caseItem.key === caseKey) {
                    return {
                        ...caseItem,
                        tasks: (caseItem.tasks || []).filter((childTask) => childTask.name !== data.name),
                    };
                }

                return caseItem;
            });
        }

        updatedTasks = workflowTasks.map((task) => {
            if (task.name !== name) {
                return task;
            }

            return parentBranchTask;
        }) as Array<WorkflowTaskType>;
    } else {
        updatedTasks = workflowTasks.filter((task: WorkflowTask) => task.name !== data.name);
    }

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(
                    {
                        ...workflowDefinition,
                        tasks: updatedTasks,
                    },
                    null,
                    SPACE
                ),
                version: workflow.version,
            },
        },
        {
            onSuccess: () => {
                queryClient.invalidateQueries({
                    queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
                        id: workflow.id!,
                        lastWorkflowNodeName: currentNode?.name,
                    }),
                });

                queryClient.invalidateQueries({
                    queryKey: ProjectWorkflowKeys.projectWorkflows(+projectId!),
                });

                queryClient.invalidateQueries({
                    queryKey: ProjectWorkflowKeys.workflows,
                });

                if (currentNode?.name === data.name) {
                    useWorkflowNodeDetailsPanelStore.getState().reset();
                    useWorkflowTestChatStore.getState().setWorkflowTestChatPanelOpen(false);
                }
            },
        }
    );
}
