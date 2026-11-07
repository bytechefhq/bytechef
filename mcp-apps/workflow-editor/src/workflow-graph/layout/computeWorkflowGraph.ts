import {WorkflowDefinitionType} from '../../types';
import {BranchCaseType, NodeDataType, ParsedWorkflowType} from '../types';
import {
    EDGE_STYLES,
    FINAL_PLACEHOLDER_NODE_ID,
    LAYOUT_DIRECTION,
    LayoutDirectionType,
    ON_ERROR_WIRE_KEY_ERROR_BRANCH,
    ON_ERROR_WIRE_KEY_MAIN_BRANCH,
} from './constants';
import createBranchEdges from './createBranchEdges';
import createBranchNode from './createBranchNode';
import createConditionEdges, {hasTaskInConditionBranches} from './createConditionEdges';
import createConditionNode from './createConditionNode';
import createEachEdges from './createEachEdges';
import createEachNode from './createEachNode';
import createForkJoinEdges from './createForkJoinEdges';
import createForkJoinNode from './createForkJoinNode';
import createLoopEdges from './createLoopEdges';
import createLoopNode from './createLoopNode';
import createMapEdges from './createMapEdges';
import createMapNode from './createMapNode';
import createOnErrorEdges, {hasTaskInOnErrorBranches} from './createOnErrorEdges';
import createOnErrorNode from './createOnErrorNode';
import createParallelEdges from './createParallelEdges';
import createParallelNode from './createParallelNode';
import {flattenDefinitionTasks} from './flattenDefinitionTasks';
import {getLayoutElements} from './getLayoutElements';
import {convertTaskToNode, createDefaultNodes, createEdgeFromTaskDispatcherBottomGhostNode} from './nodeFactory';
import {collectTaskDispatcherData, getTaskAncestry} from './taskAncestry';

import type {Edge, Node} from '@xyflow/react';

const CANVAS_WIDTH = 1000;

export function componentNameFromType(type: string): string {
    return (type ?? '').split('/')[0] ?? '';
}

function parse(workflowDefinition: WorkflowDefinitionType): ParsedWorkflowType {
    return workflowDefinition as unknown as ParsedWorkflowType;
}

export async function computeWorkflowGraph(
    workflowDefinition: WorkflowDefinitionType,
    direction: LayoutDirectionType = LAYOUT_DIRECTION
): Promise<{edges: Edge[]; nodes: Node[]}> {
    const parsed = parse(workflowDefinition);

    const tasks = flattenDefinitionTasks(parsed.tasks ?? []);
    const triggers = parsed.triggers ?? [];

    if (tasks.length === 0 && triggers.length === 0) {
        return {edges: [], nodes: []};
    }

    const triggerNode = triggers[0] ? convertTaskToNode(triggers[0], 0) : createDefaultNodes(CANVAS_WIDTH)[0];

    let allNodes: Array<Node> = [triggerNode];

    if (tasks.length > 0) {
        const branchChildTasks = {};
        const conditionChildTasks = {};
        const eachChildTasks = {};
        const forkJoinChildTasks = {};
        const loopChildTasks = {};
        const mapChildTasks = {};
        const onErrorChildTasks = {};
        const parallelChildTasks = {};

        tasks.forEach((task) => {
            collectTaskDispatcherData(
                task,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                loopChildTasks,
                mapChildTasks,
                onErrorChildTasks,
                parallelChildTasks
            );
        });

        tasks.forEach((task) => {
            const {name, parameters, type} = task;

            const componentName = type.split('/')[0];

            const taskNode = convertTaskToNode(task, 1);

            const {isNested, nestingData: detectedNestingData} = getTaskAncestry({
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                loopChildTasks,
                mapChildTasks,
                onErrorChildTasks,
                parallelChildTasks,
                taskName: name,
            });

            if (isNested) {
                taskNode.data = {
                    ...taskNode.data,
                    ...detectedNestingData,
                };
            }

            if (componentName === 'condition') {
                const hasTrueBranchTasks = parameters?.caseTrue?.length > 0;
                const hasFalseBranchTasks = parameters?.caseFalse?.length > 0;

                allNodes = createConditionNode({
                    allNodes: [...allNodes, taskNode],
                    conditionId: taskNode.id,
                    isNested,
                    options: {
                        createLeftPlaceholder: !hasTrueBranchTasks,
                        createRightPlaceholder: !hasFalseBranchTasks,
                    },
                });
            } else if (componentName === 'loop') {
                const hasSubtasks = parameters?.iteratee?.length > 0;

                allNodes = createLoopNode({
                    allNodes: [...allNodes, taskNode],
                    isNested,
                    loopId: taskNode.id,
                    options: {
                        createPlaceholder: !hasSubtasks,
                    },
                });
            } else if (componentName === 'map') {
                const hasSubtasks = parameters?.iteratee?.length > 0;

                allNodes = createMapNode({
                    allNodes: [...allNodes, taskNode],
                    isNested,
                    mapId: taskNode.id,
                    options: {
                        createPlaceholder: !hasSubtasks,
                    },
                });
            } else if (componentName === 'on-error') {
                const errorBranch = parameters?.[ON_ERROR_WIRE_KEY_ERROR_BRANCH];
                const mainBranch = parameters?.[ON_ERROR_WIRE_KEY_MAIN_BRANCH];
                const hasErrorBranchTasks = Array.isArray(errorBranch) && errorBranch.length > 0;
                const hasMainBranchTasks = Array.isArray(mainBranch) && mainBranch.length > 0;

                allNodes = createOnErrorNode({
                    allNodes: [...allNodes, taskNode],
                    isNested,
                    onErrorId: taskNode.id,
                    options: {
                        createLeftPlaceholder: !hasMainBranchTasks,
                        createRightPlaceholder: !hasErrorBranchTasks,
                    },
                });
            } else if (componentName === 'branch') {
                const hasDefaultSubtasks = parameters?.default?.length > 0;

                const casesWithoutTasks = (parameters?.cases as BranchCaseType[])?.filter(
                    (taskCase) => taskCase.tasks?.length === 0
                );

                const emptyCaseKeys = casesWithoutTasks?.map((taskCase) => taskCase.key);

                allNodes = createBranchNode({
                    allNodes: [...allNodes, taskNode],
                    branchId: taskNode.id,
                    isNested,
                    options: {
                        createDefaultPlaceholder: !hasDefaultSubtasks,
                        emptyCaseKeys,
                    },
                });
            } else if (componentName === 'parallel') {
                const hasSubtasks = parameters?.tasks?.length > 0;

                allNodes = createParallelNode({
                    allNodes: [...allNodes, taskNode],
                    isNested,
                    options: {
                        createLeftGhost: !hasSubtasks,
                    },
                    parallelId: taskNode.id,
                });
            } else if (componentName === 'each') {
                const hasSubtasks = parameters?.iteratee?.name;

                allNodes = createEachNode({
                    allNodes: [...allNodes, taskNode],
                    eachId: taskNode.id,
                    isNested,
                    options: {
                        createPlaceholder: !hasSubtasks,
                    },
                });
            } else if (componentName === 'fork-join') {
                const hasSubtasks = parameters?.branches?.length > 0;

                allNodes = createForkJoinNode({
                    allNodes: [...allNodes, taskNode],
                    forkJoinId: taskNode.id,
                    isNested,
                    options: {
                        createLeftGhost: !hasSubtasks,
                    },
                });
            } else {
                allNodes.push(taskNode);
            }
        });
    }

    const finalPlaceholderNode: Node = {
        data: {label: '+'},
        id: FINAL_PLACEHOLDER_NODE_ID,
        position: {x: 0, y: 0},
        type: 'placeholder',
    };

    const taskEdges: Array<Edge> = [];

    allNodes.forEach((node, index) => {
        const nodeData: NodeDataType = node.data as NodeDataType;

        const isBranchNode = nodeData.componentName === 'branch';
        const isConditionNode = nodeData.componentName === 'condition';
        const isEachNode = nodeData.componentName === 'each';
        const isLoopNode = nodeData.componentName === 'loop';
        const isMapNode = nodeData.componentName === 'map';
        const isParallellNode = nodeData.componentName === 'parallel';
        const isForkJoinNode = nodeData.componentName === 'fork-join';
        const isOnErrorNode = nodeData.componentName === 'on-error';

        const isConditionPlaceholderNode = nodeData.conditionId && node.type === 'placeholder';
        const isBranchPlaceholderNode = nodeData.branchId && node.type === 'placeholder';
        const isOnErrorPlaceholderNode = nodeData.onErrorId && node.type === 'placeholder';

        const isConditionChildTask = nodeData.conditionData;
        const isOnErrorChildTask = nodeData.onErrorData;

        const nextNode = allNodes[index + 1];

        if (isConditionNode) {
            const conditionEdges = createConditionEdges(node, allNodes);

            taskEdges.push(...conditionEdges);

            return;
        }

        if (isOnErrorNode) {
            const onErrorEdges = createOnErrorEdges(node, allNodes);

            taskEdges.push(...onErrorEdges);

            return;
        }

        if (isLoopNode) {
            const loopEdges = createLoopEdges(node);

            taskEdges.push(...loopEdges);

            return;
        }

        if (isMapNode) {
            const mapEdges = createMapEdges(node);

            taskEdges.push(...mapEdges);

            return;
        }

        if (isBranchNode) {
            const branchEdges = createBranchEdges(node);

            taskEdges.push(...branchEdges);

            return;
        }

        if (isParallellNode) {
            const parallelEdges = createParallelEdges(node);

            taskEdges.push(...parallelEdges);

            return;
        }

        if (isEachNode) {
            const eachEdges = createEachEdges(node);

            taskEdges.push(...eachEdges);

            return;
        }

        if (isForkJoinNode) {
            const forkJoinEdges = createForkJoinEdges(node);

            taskEdges.push(...forkJoinEdges);

            return;
        }

        if (nextNode) {
            const isNextNodeTaskDispatcherBottomNode = nextNode.type === 'taskDispatcherBottomGhostNode';

            const nextNodeData: NodeDataType = nextNode.data as NodeDataType;

            const isTaskDispatcherBottomGhostNode = node.type === 'taskDispatcherBottomGhostNode';

            if (isTaskDispatcherBottomGhostNode) {
                const edgeFromTaskDispatcherBottomGhost = createEdgeFromTaskDispatcherBottomGhostNode({
                    allNodes,
                    index,
                    node,
                    tasks,
                });

                if (edgeFromTaskDispatcherBottomGhost) {
                    const isDuplicate = taskEdges.some((edge) => edge.id === edgeFromTaskDispatcherBottomGhost.id);

                    if (!isDuplicate) {
                        taskEdges.push(edgeFromTaskDispatcherBottomGhost);

                        if (
                            edgeFromTaskDispatcherBottomGhost.target === FINAL_PLACEHOLDER_NODE_ID &&
                            !allNodes.some((currentNode) => currentNode.id === FINAL_PLACEHOLDER_NODE_ID)
                        ) {
                            allNodes.push(finalPlaceholderNode);
                        }
                    }
                }

                return;
            }

            if (isConditionChildTask || node.id.includes('condition-')) {
                const conditionId = isConditionChildTask ? nodeData.conditionData?.conditionId : node.id.split('-')[0];

                if (conditionId) {
                    const isNextNodeInSameCondition =
                        nextNodeData.conditionData?.conditionId === conditionId ||
                        hasTaskInConditionBranches(conditionId, nextNode.id, tasks);

                    const isOwnBottomGhost =
                        isNextNodeTaskDispatcherBottomNode && nextNode.id === `${conditionId}-condition-bottom-ghost`;

                    const isInDifferentBranches =
                        nodeData.conditionData &&
                        nextNodeData.conditionData &&
                        nodeData.conditionData.conditionCase !== nextNodeData.conditionData.conditionCase;

                    if (isInDifferentBranches || (!isNextNodeInSameCondition && !isOwnBottomGhost)) {
                        return;
                    }
                }
            }

            if (isOnErrorChildTask || node.id.includes('-onError-')) {
                const onErrorDispatcherId = isOnErrorChildTask ? nodeData.onErrorData?.onErrorId : nodeData.onErrorId;

                if (onErrorDispatcherId) {
                    const isNextNodeInSameOnError =
                        nextNodeData.onErrorData?.onErrorId === onErrorDispatcherId ||
                        hasTaskInOnErrorBranches(onErrorDispatcherId, nextNode.id, tasks);

                    const isOwnBottomGhost =
                        isNextNodeTaskDispatcherBottomNode &&
                        nextNode.id === `${onErrorDispatcherId}-onError-bottom-ghost`;

                    const isInDifferentOnErrorBranches =
                        nodeData.onErrorData &&
                        nextNodeData.onErrorData &&
                        nodeData.onErrorData.onErrorCase !== nextNodeData.onErrorData.onErrorCase;

                    if (isInDifferentOnErrorBranches || (!isNextNodeInSameOnError && !isOwnBottomGhost)) {
                        return;
                    }
                }
            }

            if (isConditionPlaceholderNode || isBranchPlaceholderNode || isOnErrorPlaceholderNode) {
                return;
            }

            let edgeToNextNode: Edge = {
                id: `${node.id}=>${nextNode.id}`,
                source: node.id,
                style: EDGE_STYLES,
                target: nextNode.id,
                type: node.id.includes('placeholder') ? 'smoothstep' : 'workflow',
            };

            if (isNextNodeTaskDispatcherBottomNode) {
                edgeToNextNode = {
                    ...edgeToNextNode,
                    targetHandle: `${nextNode.id}-bottom-ghost-right`,
                };
            }

            if (!taskEdges.find((edge) => edge.source === node.id)) {
                taskEdges.push(edgeToNextNode);
            }
        } else {
            allNodes.push(finalPlaceholderNode);

            taskEdges.push({
                id: `${node.id}=>${FINAL_PLACEHOLDER_NODE_ID}`,
                source: node.id,
                target: FINAL_PLACEHOLDER_NODE_ID,
                type: 'placeholder',
            });
        }
    });

    const layoutNodes = allNodes.map((node) => {
        if (node.type === 'workflow' || node.type === 'clusterRoot') {
            return {
                ...node,
                data: {
                    ...node.data,
                    clusterElements: undefined,
                    clusterRoot: undefined,
                },
                type: 'readonly',
            };
        }

        if (node.type === 'placeholder') {
            return {
                ...node,
                type: 'readonlyPlaceholder',
            };
        }

        return node;
    });

    layoutNodes.pop();

    const edges = taskEdges.map((edge) => ({
        ...edge,
        type: edge.type === 'labeledBranchCase' ? 'labeledBranchCase' : 'smoothstep',
    }));

    const lastEdge = edges[edges.length - 1];

    if (lastEdge && lastEdge.target === FINAL_PLACEHOLDER_NODE_ID) {
        edges.pop();
    }

    if (layoutNodes.length === 0) {
        return {edges: [], nodes: []};
    }

    return getLayoutElements({
        canvasWidth: CANVAS_WIDTH,
        direction,
        edges,
        nodes: layoutNodes,
    });
}
