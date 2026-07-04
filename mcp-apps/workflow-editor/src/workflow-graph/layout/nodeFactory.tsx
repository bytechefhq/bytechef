// Ported from client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx — the read-only
// slice consumed by the layout pipeline: convertTaskToNode, createDefaultNodes,
// createEdgeFromTaskDispatcherBottomGhostNode, calculateNodeHeight (plus the getBranchCaseSide
// helper the bottom-ghost edge builder needs).
//
// Adaptations:
//   - convertTaskToNode drops the (taskDefinition) param and the pre-rendered <InlineSVG> icon;
//     icons are resolved at render time from data.componentName via the component-icon proxy.
//   - createDefaultNodes drops the PlayIcon JSX; the manual trigger renders from componentName.
//   - clusterRoot nodes are treated as plain 'workflow' nodes (no AI-agent cluster canvas here).
//   - Store-free; constants/types/helpers imported locally.

import {Edge, Node} from '@xyflow/react';

import {NodeDataType, WorkflowNodeDataType, WorkflowTaskType} from '../types';
import {
    CONDITION_CASE_TRUE,
    EDGE_STYLES,
    FINAL_PLACEHOLDER_NODE_ID,
    LayoutDirectionType,
    NODE_HEIGHT,
    ON_ERROR_ERROR_BRANCH,
    ON_ERROR_MAIN_BRANCH,
    PLACEHOLDER_NODE_HEIGHT,
    TASK_DISPATCHER_NAMES,
} from './constants';
import {getConditionBranchSide} from './createConditionEdges';
import {getForkJoinBranchSide} from './createForkJoinEdges';
import {getOnErrorBranchSide} from './createOnErrorEdges';
import {TASK_DISPATCHER_CONFIG, getParentTaskDispatcherTask} from './taskDispatcherConfig';

export const calculateNodeHeight = (node: Node) => {
    const isTopGhostNode = node.type === 'taskDispatcherTopGhostNode';
    const isBottomGhostNode = node.type === 'taskDispatcherBottomGhostNode';
    const isLeftGhostNode = node.type === 'taskDispatcherLeftGhostNode';
    const isPlaceholderNode = node.type === 'placeholder';
    const isGhostNode = isTopGhostNode || isBottomGhostNode || isLeftGhostNode;

    let height = NODE_HEIGHT;

    if (isPlaceholderNode || isGhostNode) {
        height = PLACEHOLDER_NODE_HEIGHT;

        if (isTopGhostNode || isBottomGhostNode) {
            height = 0;
        }
    }

    return height;
};

export const convertTaskToNode = (task: WorkflowTaskType, index: number): Node => {
    const componentName = task.type.split('/')[0];

    const isTaskDispatcher = TASK_DISPATCHER_NAMES.includes(componentName);

    const data: WorkflowNodeDataType = {
        ...task,
        componentName,
        label: task.label ?? componentName,
        name: task.name,
        operationName: task.type.split('/')[2],
        taskDispatcher: isTaskDispatcher,
        taskDispatcherId: isTaskDispatcher ? task.name : undefined,
        title: task.label,
        trigger: index === 0,
        type: task.type,
        workflowNodeName: task.name,
    };

    return {
        data: data as unknown as Record<string, unknown>,
        id: task.name,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
};

interface CreateEdgeFromTaskDispatcherBottomGhostNodeProps {
    allNodes?: Node[];
    index?: number;
    node: Node;
    tasks?: WorkflowTaskType[];
}

export const createEdgeFromTaskDispatcherBottomGhostNode = ({
    allNodes = [],
    index = 0,
    node,
    tasks = [],
}: CreateEdgeFromTaskDispatcherBottomGhostNodeProps): Edge | null => {
    const nodeData = node.data as NodeDataType;

    const {taskDispatcherId} = nodeData;

    if (!taskDispatcherId) {
        return null;
    }

    let componentName;

    // Connect to the parent task dispatcher if this is a nested task dispatcher
    if (node.data.isNestedBottomGhost) {
        const parentTaskDispatcher = getParentTaskDispatcherTask(taskDispatcherId, tasks);

        if (!parentTaskDispatcher) {
            return null;
        }

        const taskDispatcherNode = allNodes.find((currentNode) => currentNode.id === taskDispatcherId);

        componentName = parentTaskDispatcher.type.split('/')[0];

        let parentSubtasks: WorkflowTaskType[];

        switch (componentName) {
            case 'branch': {
                parentSubtasks = TASK_DISPATCHER_CONFIG[
                    componentName as keyof typeof TASK_DISPATCHER_CONFIG
                ].getSubtasks({
                    context: {
                        caseKey: (taskDispatcherNode?.data as NodeDataType)?.branchData?.caseKey,
                        taskDispatcherId: parentTaskDispatcher.name,
                    },
                    task: parentTaskDispatcher,
                });

                break;
            }
            case 'parallel':
                return null;
            case 'condition':
                parentSubtasks = TASK_DISPATCHER_CONFIG[
                    componentName as keyof typeof TASK_DISPATCHER_CONFIG
                ].getSubtasks({
                    context: {
                        conditionCase:
                            ((taskDispatcherNode?.data as NodeDataType).conditionData?.conditionCase as
                                | 'caseTrue'
                                | 'caseFalse') || CONDITION_CASE_TRUE,
                        taskDispatcherId: parentTaskDispatcher.name,
                    },
                    task: parentTaskDispatcher,
                });

                break;
            case 'on-error':
                parentSubtasks = TASK_DISPATCHER_CONFIG['on-error'].getSubtasks({
                    context: {
                        onErrorCase:
                            ((taskDispatcherNode?.data as NodeDataType).onErrorData?.onErrorCase as
                                | typeof ON_ERROR_MAIN_BRANCH
                                | typeof ON_ERROR_ERROR_BRANCH) || ON_ERROR_MAIN_BRANCH,
                        taskDispatcherId: parentTaskDispatcher.name,
                    },
                    task: parentTaskDispatcher,
                });

                break;
            case 'fork-join': {
                const branches = parentTaskDispatcher.parameters?.branches || [];

                const branchIndex = branches.findIndex(
                    (branch: WorkflowTaskType[]) =>
                        Array.isArray(branch) && branch.some((subtask) => subtask.name === taskDispatcherId)
                );

                parentSubtasks = branchIndex !== -1 ? branches[branchIndex] || [] : [];

                break;
            }
            default: {
                parentSubtasks = TASK_DISPATCHER_CONFIG[
                    componentName as keyof typeof TASK_DISPATCHER_CONFIG
                ].getSubtasks({
                    task: parentTaskDispatcher,
                });

                break;
            }
        }

        const currentSubtaskIndex = parentSubtasks.findIndex((subtask) => subtask.name === taskDispatcherId);

        const nextSubtask = parentSubtasks[currentSubtaskIndex + 1];

        if (nextSubtask) {
            const edgeFromNestedBottomGhostToNextSubtask = {
                id: `${node.id}=>${nextSubtask.name}`,
                source: node.id,
                style: EDGE_STYLES,
                target: nextSubtask.name,
                type: 'workflow',
            };

            return edgeFromNestedBottomGhostToNextSubtask;
        }

        const parentBottomGhostSegment =
            componentName === 'fork-join' ? 'forkJoin' : componentName === 'on-error' ? 'onError' : componentName;

        const parentTaskDispatcherBottomGhostId = `${parentTaskDispatcher.name}-${parentBottomGhostSegment}-bottom-ghost`;
        const parentTaskDispatcherBottomGhost = allNodes.find(
            (currentNode) => currentNode.id === parentTaskDispatcherBottomGhostId
        );

        if (!parentTaskDispatcherBottomGhost) {
            return null;
        }

        let targetHandle = `${parentTaskDispatcherBottomGhostId}-right`;

        if (componentName === 'condition') {
            const branchSide = getConditionBranchSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${branchSide}`;
        } else if (componentName === 'on-error') {
            const branchSide = getOnErrorBranchSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${branchSide}`;
        } else if (componentName === 'fork-join') {
            const branchSide = getForkJoinBranchSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${branchSide}`;
        } else if (componentName === 'branch') {
            const branchSide = getBranchCaseSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            const handlePosition = branchSide === 'middle' ? 'top' : branchSide;

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${handlePosition}`;
        }

        return {
            id: `${node.id}=>${parentTaskDispatcherBottomGhostId}`,
            source: node.id,
            style: EDGE_STYLES,
            target: parentTaskDispatcherBottomGhostId,
            targetHandle,
            type: 'workflow',
        };
    }

    const subsequentNodes = allNodes.slice(index + 1);

    const nextTaskNodeOutsideTaskDispatcher = subsequentNodes.find((subsequentNode) => {
        if (subsequentNode.type !== 'workflow' && subsequentNode.type !== 'clusterRoot') {
            return false;
        }

        const subsequentNodeData = subsequentNode.data as NodeDataType;

        if (subsequentNodeData.conditionData && subsequentNodeData.conditionData.conditionId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.onErrorData && subsequentNodeData.onErrorData.onErrorId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.loopData && subsequentNodeData.loopData.loopId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.branchData && subsequentNodeData.branchData.branchId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.parallelData && subsequentNodeData.parallelData.parallelId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.eachData && subsequentNodeData.eachData.eachId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.forkJoinData && subsequentNodeData.forkJoinData.forkJoinId === taskDispatcherId) {
            return false;
        } else if (
            subsequentNodeData.terminateData &&
            subsequentNodeData.terminateData.terminateId === taskDispatcherId
        ) {
            return false;
        }

        for (const task of tasks || []) {
            const taskComponentName = task.type?.split('/')[0];

            if (!TASK_DISPATCHER_NAMES.includes(taskComponentName)) {
                continue;
            }

            const subtasks = TASK_DISPATCHER_CONFIG[
                taskComponentName as keyof typeof TASK_DISPATCHER_CONFIG
            ].getSubtasks({
                getAllSubtasks: true,
                task,
            });

            if (Array.isArray(subtasks) && subtasks.some((subtask) => subtask.name === subsequentNode.id)) {
                return false;
            }
        }

        return true;
    });

    if (nextTaskNodeOutsideTaskDispatcher) {
        return {
            id: `${node.id}=>${nextTaskNodeOutsideTaskDispatcher.id}`,
            source: node.id,
            style: EDGE_STYLES,
            target: nextTaskNodeOutsideTaskDispatcher.id,
            type: 'workflow',
        };
    }

    return {
        id: `${node.id}=>${FINAL_PLACEHOLDER_NODE_ID}`,
        source: node.id,
        style: EDGE_STYLES,
        target: FINAL_PLACEHOLDER_NODE_ID,
        type: 'placeholder',
    };
};

/**
 * Determines the target handle side for a branch case based on case position
 */
export function getBranchCaseSide(
    taskDispatcherId: string,
    tasks: WorkflowTaskType[],
    parentBranchId: string
): 'left' | 'middle' | 'right' {
    const parentBranchTask = tasks?.find((task) => task.name === parentBranchId);

    if (!parentBranchTask) {
        return 'right';
    }

    const defaultCase = {
        key: 'default',
        tasks: parentBranchTask.parameters?.default || [],
    };

    const customCases = (parentBranchTask.parameters?.cases || []).map(
        (caseItem: {key: string | number; tasks?: WorkflowTaskType[]}) => ({
            key: caseItem.key,
            tasks: caseItem.tasks || [],
        })
    );

    const allCases = [defaultCase, ...customCases];

    const caseIndex = allCases.findIndex((caseItem) =>
        caseItem.tasks.some((task: WorkflowTaskType) => task.name === taskDispatcherId)
    );

    if (caseIndex === -1) {
        return 'right';
    }

    const isEvenCount = allCases.length % 2 === 0;

    if (isEvenCount) {
        const halfPoint = allCases.length / 2;

        if (caseIndex < halfPoint) {
            return 'left';
        } else {
            return 'right';
        }
    } else {
        const middleIndex = Math.floor(allCases.length / 2);

        if (caseIndex < middleIndex) {
            return 'left';
        } else if (caseIndex === middleIndex) {
            return 'middle';
        } else {
            return 'right';
        }
    }
}

export const createDefaultNodes = (canvasWidth: number, direction: LayoutDirectionType = 'TB'): Node[] => [
    {
        data: {
            componentName: 'manual',
            id: 'manual',
            label: 'Manual',
            name: 'manual',
            operationName: 'manual',
            trigger: true,
            type: 'manual/v1/manual',
            workflowNodeName: 'trigger_1',
        },
        id: 'trigger_1',
        position: direction === 'LR' ? {x: 50, y: canvasWidth / 2 - 36} : {x: canvasWidth / 2 - 36, y: 50},
        type: 'workflow',
    },
    {
        data: {label: '+'},
        id: FINAL_PLACEHOLDER_NODE_ID,
        position: direction === 'LR' ? {x: 150, y: canvasWidth / 2 - 36} : {x: canvasWidth / 2 - 36, y: 150},
        type: 'placeholder',
    },
];
