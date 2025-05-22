import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {Edge, Node} from '@xyflow/react';
import {useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowEditorStore from '../../workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../../workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {getLayoutedElements} from '../../workflow-editor/utils/layoutUtils';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import {convertNameToCamelCase} from '../utils/clusterElementsUtils';
import {
    createEdgeForClusterElementNode,
    createEdgeForMultipleElementsGhostNode,
    createEdgeForMultipleElementsPlaceholderNode,
    createEdgeForPlaceholderNode,
    createEdgeMultipleElementsNode,
} from '../utils/createClusterElementsEdges';
import {
    createMultipleElementsGhostNode,
    createMultipleElementsNode,
    createPlaceholderNode,
    createSingleElementsNode,
} from '../utils/createClusterElementsNodes';

const useClusterElementsLayout = () => {
    const {rootClusterElementNodeData} = useWorkflowEditorStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const componentVersion = rootClusterElementNodeData?.type
        ? parseInt(rootClusterElementNodeData.type.split('/')[1].replace(/^v/, ''))
        : 1;

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: rootClusterElementNodeData?.componentName || '',
            componentVersion: componentVersion || 1,
        },
        !!rootClusterElementNodeData && currentNode?.rootClusterElement
    );

    const {setEdges, setNodes} = useClusterElementsDataStore(
        useShallow((state) => ({
            setEdges: state.setEdges,
            setNodes: state.setNodes,
        }))
    );

    const canvasWidth = window.innerWidth - 120 - 460;

    const {allNodes, taskEdges} = useMemo(() => {
        const nodes: Array<Node> = [];
        const edges: Array<Edge> = [];

        if (!rootClusterElementNodeData) {
            return {allNodes: nodes, taskEdges: edges};
        }

        if (rootClusterElementNodeData) {
            const rootClusterElementNode = {
                data: rootClusterElementNodeData,
                id: rootClusterElementNodeData.workflowNodeName,
                position: {x: 0, y: 0},
                type: 'workflow',
            };

            nodes.push(rootClusterElementNode);
        }

        const clusterElements = rootClusterElementNodeData.clusterElements || {};

        //Create nodes
        if (rootClusterElementDefinition?.clusterElementTypes && clusterElements) {
            rootClusterElementDefinition.clusterElementTypes.forEach((clusterElementType) => {
                const elementType = convertNameToCamelCase(clusterElementType.name as string);
                const elementLabel = clusterElementType?.label;
                const isMultipleElementsNode = clusterElementType.multipleElements;
                const currentRootClusterElementNodeName = rootClusterElementNodeData.workflowNodeName;
                const clusterElementData = clusterElements[elementType as keyof typeof clusterElements];

                if (isMultipleElementsNode) {
                    nodes.push(
                        createMultipleElementsGhostNode(
                            currentRootClusterElementNodeName,
                            elementLabel,
                            elementType,
                            isMultipleElementsNode
                        )
                    );

                    if (Array.isArray(clusterElementData) && clusterElementData.length) {
                        clusterElementData.forEach((element) => {
                            nodes.push(createMultipleElementsNode(element, elementType, isMultipleElementsNode));
                        });
                    }

                    nodes.push(createPlaceholderNode(currentRootClusterElementNodeName, elementLabel, elementType));
                } else {
                    if (clusterElementData && !Array.isArray(clusterElementData)) {
                        nodes.push(createSingleElementsNode(clusterElementData, elementLabel, elementType));
                    } else {
                        nodes.push(createPlaceholderNode(currentRootClusterElementNodeName, elementLabel, elementType));
                    }
                }
            });
        }

        const multipleElementNodes = nodes.filter(
            (node) => node.data.multipleClusterElementsNode && node.type === 'workflow'
        );

        //Create edges
        nodes.forEach((node) => {
            const currentNodeId = node.id;
            const isRootClusterElementNode = node.data.clusterElements;
            //Create edges from root node to cluster elements
            if (isRootClusterElementNode && rootClusterElementDefinition?.clusterElementTypes && clusterElements) {
                rootClusterElementDefinition.clusterElementTypes.forEach((clusterElementType) => {
                    const elementType = convertNameToCamelCase(clusterElementType.name as string);
                    const isMultipleElementsNode = clusterElementType.multipleElements;
                    const clusterElementData = clusterElements[elementType as keyof typeof clusterElements];

                    if (isMultipleElementsNode) {
                        const edgeFromRootToMultipleElementsGhostNode = createEdgeForMultipleElementsGhostNode(
                            currentNodeId,
                            elementType
                        );

                        edges.push(edgeFromRootToMultipleElementsGhostNode);
                    } else {
                        const targetNode = nodes.find((node) => node.data.clusterElementType === elementType);

                        if (clusterElementData && !Array.isArray(clusterElementData) && targetNode) {
                            const edgeFromRootToClusterElementNode = createEdgeForClusterElementNode(
                                currentNodeId,
                                targetNode
                            );

                            edges.push(edgeFromRootToClusterElementNode);
                        } else {
                            const edgeFromRootToPlaceholderNode = createEdgeForPlaceholderNode(
                                currentNodeId,
                                elementType
                            );

                            edges.push(edgeFromRootToPlaceholderNode);
                        }
                    }
                });
            }

            //Create edges from multiple elements ghost node to its nodes
            if (node.type === 'multipleClusterElementsGhostNode') {
                const rootNodeId = node.data.rootNodeId as string;
                const elementType = node.data.clusterElementType as string;

                //Create edges from ghost node to its nodes
                if (multipleElementNodes.length) {
                    multipleElementNodes.forEach((node) => {
                        const multipleElementNodesId = node.id;

                        const edgeFromGhostNodeToClusterElementNode = createEdgeMultipleElementsNode(
                            rootNodeId,
                            currentNodeId,
                            multipleElementNodesId
                        );

                        edges.push(edgeFromGhostNodeToClusterElementNode);
                    });
                }

                //Create edges from ghost node to multiple elements placeholder node
                const edgeFromGhostNodeToPlaceholderNode = createEdgeForMultipleElementsPlaceholderNode(
                    currentNodeId,
                    elementType,
                    rootNodeId
                );

                edges.push(edgeFromGhostNodeToPlaceholderNode);
            }
        });

        return {allNodes: nodes, taskEdges: edges};
    }, [rootClusterElementNodeData, rootClusterElementDefinition]);

    useEffect(() => {
        const layoutNodes = allNodes;
        const edges: Edge[] = taskEdges;

        const elements = getLayoutedElements({canvasWidth, edges, isClusterElementsCanvas: true, nodes: layoutNodes});

        setNodes(elements.nodes);
        setEdges(elements.edges);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [canvasWidth, rootClusterElementNodeData]);
};

export default useClusterElementsLayout;
