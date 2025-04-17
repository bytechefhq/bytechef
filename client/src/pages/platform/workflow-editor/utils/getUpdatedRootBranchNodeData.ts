import {BranchCaseType, NodeDataType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

interface UpdateRootBranchNodeProps {
    isStructuralUpdate?: boolean;
    updatedParentNodeData: NodeDataType;
}

export default function getUpdatedRootBranchNodeData({
    isStructuralUpdate = false,
    updatedParentNodeData,
}: UpdateRootBranchNodeProps): NodeDataType {
    const {nodes, workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        console.error('No workflow definition found');

        return updatedParentNodeData;
    }

    const nodesMap = new Map(nodes.map((node) => [node.id, node]));
    const definitionTasks = JSON.parse(workflow.definition).tasks;
    const workflowTasks = workflow.tasks;

    let currentTaskNodeData = updatedParentNodeData;
    let currentTaskNodeBranchData = updatedParentNodeData.branchData;
    let currentNodeName = updatedParentNodeData.workflowNodeName;

    while (currentTaskNodeBranchData) {
        const parentBranchTask = TASK_DISPATCHER_CONFIG.branch.getTask({
            taskDispatcherId: currentTaskNodeBranchData.branchId,
            tasks: definitionTasks,
        });

        if (!parentBranchTask) {
            console.error('No parent branch task found for branchId: ', currentTaskNodeBranchData.branchId);

            break;
        }

        const parentBranchTaskNode = nodesMap.get(parentBranchTask.name);

        if (!parentBranchTaskNode) {
            console.error('No parent branch task node found for task: ', parentBranchTask.name);

            break;
        }

        const currentBranchCase = currentTaskNodeBranchData.caseKey;
        const parentNodeData = parentBranchTaskNode.data as NodeDataType;

        if (isStructuralUpdate) {
            const parentSubtasks = TASK_DISPATCHER_CONFIG.branch.getSubtasks({
                context: {
                    caseKey: currentBranchCase,
                    taskDispatcherId: currentTaskNodeBranchData.branchId,
                },
                node: parentBranchTaskNode,
            });

            const taskIndex = parentSubtasks.findIndex((task) => task.name === currentNodeName);

            if (taskIndex >= 0) {
                // Create updated subtasks array with the updated task
                const updatedSubtasks = [...parentSubtasks];
                updatedSubtasks[taskIndex] = {
                    ...updatedSubtasks[taskIndex],
                    parameters: currentTaskNodeData.parameters,
                };

                // Update the parent's parameters using the standard pattern
                if (currentBranchCase === 'default') {
                    parentNodeData.parameters = {
                        ...parentNodeData.parameters,
                        default: updatedSubtasks,
                    };
                } else {
                    const updatedCaseIndex = parentNodeData.parameters?.cases?.findIndex(
                        (caseItem: BranchCaseType) => caseItem.key === currentBranchCase
                    );

                    if (updatedCaseIndex !== undefined && updatedCaseIndex >= 0) {
                        parentNodeData.parameters = {
                            ...parentNodeData.parameters,
                            cases: [
                                ...(parentNodeData.parameters?.cases?.slice(0, updatedCaseIndex) ?? []),
                                {
                                    key: currentBranchCase,
                                    tasks: updatedSubtasks,
                                },
                                ...(parentNodeData.parameters?.cases?.slice(updatedCaseIndex + 1) ?? []),
                            ],
                        };
                    }
                }
            }
        } else {
            const parentSubtasks = TASK_DISPATCHER_CONFIG.branch.getSubtasks({
                context: {
                    caseKey: currentBranchCase,
                    taskDispatcherId: currentTaskNodeBranchData.branchId,
                },
                node: parentBranchTaskNode,
            });

            const currentTask = workflowTasks?.find((task) => task.name === currentTaskNodeData.workflowNodeName);

            if (!currentTask) {
                console.error('No current task found for node: ', currentTaskNodeData.workflowNodeName);

                break;
            }

            const currentTaskIndex = parentSubtasks.findIndex((task) => task.name === currentTask.name) ?? 0;
            parentSubtasks[currentTaskIndex] = currentTask;

            const updatedCaseIndex = parentNodeData.parameters?.cases?.findIndex(
                (caseItem: BranchCaseType) => caseItem.key === currentBranchCase
            );

            if (currentBranchCase === 'default') {
                parentNodeData.parameters = {
                    ...parentNodeData.parameters,
                    default: parentSubtasks,
                };
            } else {
                parentNodeData.parameters = {
                    ...parentNodeData.parameters,
                    cases: [
                        ...(parentNodeData.parameters?.cases?.slice(0, updatedCaseIndex) ?? []),
                        {
                            key: currentBranchCase,
                            tasks: parentSubtasks,
                        },
                        ...(parentNodeData.parameters?.cases?.slice(updatedCaseIndex + 1) ?? []),
                    ],
                };
            }
        }

        currentNodeName = parentBranchTask.name;
        currentTaskNodeData = parentBranchTaskNode.data as NodeDataType;
        currentTaskNodeBranchData = currentTaskNodeData.branchData;
    }

    return currentTaskNodeData;
}
