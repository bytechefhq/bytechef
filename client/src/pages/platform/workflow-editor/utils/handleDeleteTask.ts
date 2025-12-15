import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {SPACE} from '@/shared/constants';
import {Workflow, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {BranchCaseType, NodeDataType, WorkflowDefinitionType, WorkflowTaskType} from '@/shared/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

import {WorkflowDataType} from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import findAndRemoveClusterElement from './findAndRemoveClusterElement';
import getRecursivelyUpdatedTasks from './getRecursivelyUpdatedTasks';
import {getTask} from './getTask';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

interface HandleDeleteTaskProps {
    rootClusterElementNodeData?: NodeDataType;
    clusterElementsCanvasOpen?: boolean;
    currentNode?: NodeDataType;
    data: NodeDataType;
    invalidateWorkflowQueries: () => void;
    queryClient: QueryClient;
    setRootClusterElementNodeData?: (node: NodeDataType) => void;
    setCurrentNode?: (node: NodeDataType) => void;
    updateWorkflowMutation: UseMutationResult<void, unknown, {id: string; workflow: Workflow}>;
    workflow: Workflow & WorkflowDataType;
}

export default function handleDeleteTask({
    clusterElementsCanvasOpen,
    currentNode,
    data,
    invalidateWorkflowQueries,
    queryClient,
    rootClusterElementNodeData,
    setCurrentNode,
    setRootClusterElementNodeData,
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
        const parentConditionTask = TASK_DISPATCHER_CONFIG.condition.getTask({
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
        const parentLoopTask = TASK_DISPATCHER_CONFIG.loop.getTask({
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
        const parentBranchTask = TASK_DISPATCHER_CONFIG.branch.getTask({
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
    } else if (data.parallelData) {
        const parentParallelTask = TASK_DISPATCHER_CONFIG.parallel.getTask({
            taskDispatcherId: data.parallelData.parallelId,
            tasks: workflowTasks,
        });

        if (!parentParallelTask?.parameters) {
            return;
        }

        parentParallelTask.parameters.tasks = (parentParallelTask.parameters.tasks as Array<WorkflowTask>).filter(
            (childTask) => childTask.name !== data.name
        );

        updatedTasks = workflowTasks.map((task) => {
            if (task.name !== parentParallelTask.name) {
                return task;
            }

            return parentParallelTask;
        }) as Array<WorkflowTaskType>;
    } else if (data.eachData) {
        const parentEachTask = TASK_DISPATCHER_CONFIG.loop.getTask({
            taskDispatcherId: data.eachData.eachId,
            tasks: workflowTasks,
        });

        if (!parentEachTask?.parameters) {
            return;
        }

        parentEachTask.parameters.iteratee = {};

        updatedTasks = workflowTasks.map((task) => {
            if (task.name !== parentEachTask.name) {
                return task;
            }

            return parentEachTask;
        }) as Array<WorkflowTaskType>;
    } else if (data.forkJoinData) {
        const parentForkJoinTask = TASK_DISPATCHER_CONFIG['fork-join'].getTask({
            taskDispatcherId: data.forkJoinData.forkJoinId,
            tasks: workflowTasks,
        });

        if (!parentForkJoinTask?.parameters) {
            return;
        }

        parentForkJoinTask.parameters.branches = parentForkJoinTask.parameters.branches
            .map((branch: Array<WorkflowTask>) => branch.filter((task: WorkflowTask) => task.name !== data.name))
            .filter((branch: Array<WorkflowTask>) => branch.length > 0);

        updatedTasks = workflowTasks.map((task) => {
            if (task.name !== parentForkJoinTask.name) {
                return task;
            }

            return parentForkJoinTask;
        }) as Array<WorkflowTaskType>;
    } else if (clusterElementsCanvasOpen && rootClusterElementNodeData) {
        const mainRootClusterElementTask = getTask({
            tasks: workflowTasks,
            workflowNodeName: rootClusterElementNodeData.name,
        });

        if (!mainRootClusterElementTask || !mainRootClusterElementTask.clusterElements) {
            return;
        }

        const clusterElementRemovalResult = findAndRemoveClusterElement({
            clickedElementName: data.name,
            clickedElementType: data.clusterElementType,
            clusterElements: mainRootClusterElementTask.clusterElements,
        });

        const updatedRootClusterElementTask = {
            ...mainRootClusterElementTask,
            clusterElements: clusterElementRemovalResult.elements,
        };

        if (clusterElementRemovalResult.elementFound) {
            const updatedClusterElements = clusterElementRemovalResult.elements;

            if (setRootClusterElementNodeData && setCurrentNode) {
                if (currentNode?.clusterRoot && !currentNode.isNestedClusterRoot) {
                    setCurrentNode({
                        ...currentNode,
                        clusterElements: updatedClusterElements,
                    });
                }

                setRootClusterElementNodeData({
                    ...rootClusterElementNodeData,
                    clusterElements: updatedClusterElements,
                });

                if (currentNode?.name === data.name) {
                    useWorkflowNodeDetailsPanelStore.getState().setWorkflowNodeDetailsPanelOpen(true);

                    setCurrentNode({
                        ...rootClusterElementNodeData,
                        clusterElements: updatedClusterElements,
                    });
                }
            }
        }

        // Check if the task is at top level
        const topLevelTaskIndex = workflowTasks.findIndex((task) => task.name === mainRootClusterElementTask.name);

        if (topLevelTaskIndex !== -1) {
            updatedTasks = workflowTasks.map((task) => {
                if (task.name !== mainRootClusterElementTask?.name) {
                    return task;
                }

                return updatedRootClusterElementTask;
            }) as Array<WorkflowTaskType>;
        } else {
            updatedTasks = getRecursivelyUpdatedTasks(
                workflowTasks as Array<WorkflowTask>,
                updatedRootClusterElementTask
            ) as Array<WorkflowTaskType>;
        }
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
                        environmentId: environmentStore.getState().currentEnvironmentId,
                        id: workflow.id!,
                        lastWorkflowNodeName: currentNode?.name,
                    }),
                });

                invalidateWorkflowQueries();

                if (currentNode?.name === data.name && !currentNode?.clusterElementType) {
                    useWorkflowNodeDetailsPanelStore.getState().reset();
                    useWorkflowTestChatStore.getState().setWorkflowTestChatPanelOpen(false);
                }
            },
        }
    );
}
