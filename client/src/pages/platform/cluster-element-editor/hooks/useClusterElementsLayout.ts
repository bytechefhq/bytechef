import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {Edge, Node} from '@xyflow/react';
import {useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowDataStore from '../../workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../../workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {getLayoutedElements} from '../../workflow-editor/utils/layoutUtils';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import {convertNameToCamelCase} from '../utils/clusterElementsUtils';
import {
    createEdgeForClusterElementNode,
    createEdgeForMultipleClusterElementNode,
    createEdgeForPlaceholderNode,
} from '../utils/createClusterElementsEdges';
import {
    createMultipleElementsNode,
    createPlaceholderNode,
    createSingleElementsNode,
} from '../utils/createClusterElementsNodes';

const useClusterElementsLayout = () => {
    const {rootClusterElementNodeData} = useWorkflowEditorStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();
    const {workflow} = useWorkflowDataStore.getState();

    const rootClusterElementComponentVersion =
        Number(rootClusterElementNodeData?.type?.split('/')[1].replace(/^v/, '')) || 1;

    const rootClusterElementComponentName = rootClusterElementNodeData?.componentName || '';

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: rootClusterElementComponentName,
            componentVersion: rootClusterElementComponentVersion,
        },
        !!rootClusterElementNodeData && currentNode?.rootClusterElement
    );

    const {nodes, setEdges, setNodes} = useClusterElementsDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
            setEdges: state.setEdges,
            setNodes: state.setNodes,
        }))
    );

    const nodePositions = nodes.reduce<Record<string, {x: number; y: number}>>((accumulator, node) => {
        accumulator[node.id] = {
            x: node.position.x,
            y: node.position.y,
        };
        return accumulator;
    }, {});

    const canvasWidth = window.innerWidth - 80;

    const {allNodes, taskEdges} = useMemo(() => {
        const nodes: Array<Node> = [];
        const edges: Array<Edge> = [];

        if (!rootClusterElementNodeData || !rootClusterElementDefinition || !workflow.definition) {
            return {allNodes: nodes, taskEdges: edges};
        }

        if (rootClusterElementNodeData) {
            const rootClusterElementNode = {
                data: {...rootClusterElementNodeData},
                id: rootClusterElementNodeData.workflowNodeName,
                position:
                    rootClusterElementNodeData.metadata?.ui?.nodePosition ||
                    nodePositions[rootClusterElementNodeData.workflowNodeName] ||
                    DEFAULT_NODE_POSITION,
                type: 'workflow',
            };

            nodes.push(rootClusterElementNode);
        }

        const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

        const currentClusterRootTask = workflowDefinitionTasks.find(
            (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
        );

        const clusterElements = currentClusterRootTask.clusterElements || {};

        // Create nodes
        if (rootClusterElementDefinition.clusterElementTypes && clusterElements) {
            rootClusterElementDefinition.clusterElementTypes.forEach((clusterElementType) => {
                const elementType = convertNameToCamelCase(clusterElementType.name || '');
                const elementLabel = clusterElementType.label || '';
                const isMultipleElementsNode = clusterElementType.multipleElements;
                const currentRootClusterElementNodeName = rootClusterElementNodeData.workflowNodeName;
                const clusterElementData = clusterElements[elementType as keyof typeof clusterElements];
                const rootPlaceholderPositions = rootClusterElementNodeData.metadata?.ui?.placeholderPositions || {};

                if (isMultipleElementsNode) {
                    if (Array.isArray(clusterElementData) && clusterElementData.length) {
                        clusterElementData.forEach((element) => {
                            nodes.push(
                                createMultipleElementsNode(element, elementType, isMultipleElementsNode, nodePositions)
                            );
                        });
                    }

                    nodes.push(
                        createPlaceholderNode(
                            currentRootClusterElementNodeName,
                            elementLabel,
                            elementType,
                            rootPlaceholderPositions,
                            nodePositions
                        )
                    );
                } else {
                    if (clusterElementData) {
                        nodes.push(
                            createSingleElementsNode(clusterElementData, elementLabel, elementType, nodePositions)
                        );
                    } else {
                        nodes.push(
                            createPlaceholderNode(
                                currentRootClusterElementNodeName,
                                elementLabel,
                                elementType,
                                nodePositions,
                                rootPlaceholderPositions
                            )
                        );
                    }
                }
            });
        }

        const multipleElementNodes = nodes.filter(
            (node) => node.data.multipleClusterElementsNode && node.type === 'workflow'
        );

        // Create edges
        nodes.forEach((node) => {
            const currentNodeId = node.id;
            const isRootClusterElementNode = node.data.clusterElements;

            // Create edges from root node to cluster elements
            if (isRootClusterElementNode && rootClusterElementDefinition?.clusterElementTypes && clusterElements) {
                rootClusterElementDefinition.clusterElementTypes.forEach((clusterElementType) => {
                    const elementType = convertNameToCamelCase(clusterElementType.name || '');
                    const isMultipleElementsNode = clusterElementType.multipleElements;
                    const clusterElementData = clusterElements[elementType as keyof typeof clusterElements];

                    const targetNode = nodes.find((node) => node.data.clusterElementType === elementType);

                    if (clusterElementData && !Array.isArray(clusterElementData) && targetNode) {
                        const edgeFromRootToClusterElementNode = createEdgeForClusterElementNode(
                            currentNodeId,
                            targetNode
                        );

                        edges.push(edgeFromRootToClusterElementNode);
                    } else {
                        const edgeFromRootToPlaceholderNode = createEdgeForPlaceholderNode(currentNodeId, elementType);

                        edges.push(edgeFromRootToPlaceholderNode);
                    }

                    if (isMultipleElementsNode && multipleElementNodes.length) {
                        multipleElementNodes.forEach((node) => {
                            const edgeFromRootToMultipleClusterElementNode = createEdgeForMultipleClusterElementNode(
                                currentNodeId,
                                node
                            );

                            edges.push(edgeFromRootToMultipleClusterElementNode);
                        });
                    }
                });
            }
        });

        return {allNodes: nodes, taskEdges: edges};

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [rootClusterElementNodeData, rootClusterElementDefinition, workflow]);

    useEffect(() => {
        const layoutNodes = allNodes;
        const edges: Edge[] = taskEdges;

        const elements = getLayoutedElements({canvasWidth, edges, isClusterElementsCanvas: true, nodes: layoutNodes});

        setNodes(elements.nodes);
        setEdges(elements.edges);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [canvasWidth, rootClusterElementNodeData, allNodes]);
};

export default useClusterElementsLayout;
