import {LayoutDirectionType, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {GraphNodeType, NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutEngineStore from '../stores/useLayoutEngineStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import extractNextTargets, {ExtractNextTargetsResultI} from '../utils/extractNextTargets';
import {addGraphNode, deleteGraphNode, renameGraphNode, validateGraphNodeName} from '../utils/graphNodeMutations';
import orderGraphNodeIndexes from '../utils/orderGraphNodeIndexes';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import computeBranchCaseLabelPosition from './computeBranchCaseLabelPosition';
import getRecursivelyUpdatedRootTaskDispatcherNodeData from './getRecursivelyUpdatedRootTaskDispatcherNodeData';

interface UseGraphNodeLabelProps {
    edgeId: string;
    layoutDirection: LayoutDirectionType;
    nodeIndex: number;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

export default function useGraphNodeLabel({
    edgeId,
    layoutDirection,
    nodeIndex,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: UseGraphNodeLabelProps) {
    const [isNameEditable, setIsNameEditable] = useState(false);
    const [isDeleteConfirmationVisible, setIsDeleteConfirmationVisible] = useState(false);
    const [nameValue, setNameValue] = useState('');
    const [nameError, setNameError] = useState<string | undefined>(undefined);

    const inputRef = useRef<HTMLInputElement>(null);

    const labelPosition = useMemo(
        () => computeBranchCaseLabelPosition({layoutDirection, sourceX, sourceY, targetX, targetY}),
        [layoutDirection, sourceX, sourceY, targetX, targetY]
    );

    const {nodes, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
            workflow: state.workflow,
        }))
    );

    const {updateWorkflowMutation} = useWorkflowEditor();

    const lastAppliedLayoutEngine = useLayoutEngineStore((state) => state.lastAppliedLayoutEngine);

    const targetNodeId = useMemo(() => edgeId.split('=>')[1], [edgeId]);

    const targetNodeData = useMemo(
        () => nodes.find((node) => node.id === targetNodeId)?.data as NodeDataType,
        [nodes, targetNodeId]
    );

    const parentGraphNodeId = targetNodeData?.graphData ? targetNodeData.graphData.graphId : targetNodeData?.graphId;

    const parentGraphNodeData = nodes.find((node) => node.id === parentGraphNodeId)?.data as NodeDataType;

    const graphNodes: Array<GraphNodeType> = useMemo(
        () => parentGraphNodeData?.parameters?.nodes ?? [],
        [parentGraphNodeData?.parameters?.nodes]
    );

    const graphNode: GraphNodeType | undefined = graphNodes[nodeIndex];

    const declaredNodeNames = useMemo(() => graphNodes.map((node) => node.name), [graphNodes]);

    const transitionTargets: ExtractNextTargetsResultI = useMemo(
        () => extractNextTargets(graphNode?.next, declaredNodeNames),
        [graphNode?.next, declaredNodeNames]
    );

    // "Last" means the lane the user sees on the far end, next to the trailing add-node
    // placeholder — not the last DECLARED node. Under ELK the lanes render in transition-topology
    // order, so those differ and the add button would otherwise strand itself mid-container on
    // whichever lane happens to be declared last. Dagre keeps declaration order, where the two
    // coincide, and an empty permutation falls back to the declaration index.
    const isLastNode = useMemo(() => {
        if (graphNodes.length === 0) {
            return false;
        }

        if (lastAppliedLayoutEngine !== 'elk') {
            return nodeIndex === graphNodes.length - 1;
        }

        const visualOrder = orderGraphNodeIndexes(
            graphNodes,
            parentGraphNodeData?.parameters?.startNode as string | undefined
        );

        return visualOrder.length > 0
            ? visualOrder[visualOrder.length - 1] === nodeIndex
            : nodeIndex === graphNodes.length - 1;
    }, [graphNodes, lastAppliedLayoutEngine, nodeIndex, parentGraphNodeData?.parameters?.startNode]);

    const saveGraphChange = useCallback(
        (updatedNodes: Array<GraphNodeType>) => {
            if (!workflow.definition || !parentGraphNodeData) {
                return;
            }

            const workflowDefinition = workflow.definition;
            const workflowDefinitionTasks = JSON.parse(workflowDefinition).tasks;
            const nodesMap: Map<string, Node> = new Map();

            nodes.forEach((node) => {
                const {data, id} = node;

                nodesMap.set(id, {
                    ...node,
                    data: {
                        ...data,
                        ...TASK_DISPATCHER_NAMES.reduce((nodeData: {[key: string]: unknown}, taskDispatcherName) => {
                            const key = `${taskDispatcherName}Data`;

                            if (data[key]) {
                                nodeData[key] = data[key];
                            }

                            return nodeData;
                        }, {}),
                    } as NodeDataType,
                    id,
                });
            });

            const taskNode = nodes.find(
                (node) => node.id === parentGraphNodeData?.name || node.data.name === parentGraphNodeData?.name
            );

            if (!taskNode) {
                return;
            }

            const updatedTaskNodeData: NodeDataType = {
                ...(taskNode.data as NodeDataType),
                parameters: {
                    ...parentGraphNodeData.parameters,
                    nodes: updatedNodes,
                },
            };

            const rootNodeData = getRecursivelyUpdatedRootTaskDispatcherNodeData(
                updatedTaskNodeData,
                workflowDefinitionTasks,
                nodesMap
            );

            saveWorkflowDefinition({
                nodeData: rootNodeData,
                updateWorkflowMutation: updateWorkflowMutation!,
            });
        },
        [nodes, parentGraphNodeData, updateWorkflowMutation, workflow.definition]
    );

    const handleAddNodeClick = useCallback(() => {
        saveGraphChange(addGraphNode(graphNodes));
    }, [graphNodes, saveGraphChange]);

    const handleDeleteNodeClick = useCallback(() => {
        saveGraphChange(deleteGraphNode(graphNodes, nodeIndex));
    }, [graphNodes, nodeIndex, saveGraphChange]);

    const handleEditNameClick = useCallback(() => {
        setIsNameEditable(!isNameEditable);

        setNameError(undefined);

        if (!isNameEditable) {
            setTimeout(() => {
                inputRef.current?.focus();

                inputRef.current?.select();
            }, 0);
        }
    }, [isNameEditable]);

    const handleSaveNameClick = useCallback(() => {
        if (!isNameEditable) {
            return;
        }

        const validation = validateGraphNodeName(graphNodes, nodeIndex, nameValue);

        if (!validation.valid) {
            setNameError(validation.error);

            return;
        }

        setIsNameEditable(false);
        setNameError(undefined);

        saveGraphChange(renameGraphNode(graphNodes, nodeIndex, nameValue));
    }, [graphNodes, isNameEditable, nameValue, nodeIndex, saveGraphChange]);

    const handleDeleteButtonClick = useCallback(() => {
        if (isDeleteConfirmationVisible) {
            handleDeleteNodeClick();
        } else {
            setIsDeleteConfirmationVisible(true);
        }
    }, [handleDeleteNodeClick, isDeleteConfirmationVisible]);

    useEffect(() => setNameValue(graphNode?.name ?? ''), [graphNode?.name]);

    return {
        dangling: transitionTargets.dangling,
        dynamic: transitionTargets.dynamic,
        handleAddNodeClick,
        handleDeleteButtonClick,
        handleEditNameClick,
        handleSaveNameClick,
        inputRef,
        isDeleteConfirmationVisible,
        isLastNode,
        isNameEditable,
        labelPosition,
        nameError,
        nameValue,
        setNameValue,
        targets: transitionTargets.targets,
    };
}
