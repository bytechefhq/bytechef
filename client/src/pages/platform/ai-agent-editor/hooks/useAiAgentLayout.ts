import {FINAL_PLACEHOLDER_NODE_ID, SORTED_CLUSTER_ELEMENTS_KEYS} from '@/shared/constants';
import {Edge, Node} from '@xyflow/react';
import {useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowNodeDetailsPanelStore from '../../workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {getLayoutedElements} from '../../workflow-editor/utils/layoutUtils';
import useAiAgentDataStore from '../stores/useAiAgentDataStore';
import {
    createEdgeForClusterElementNode,
    createEdgeForFinalToolPlaceholderNode,
    createEdgeForNextToolNode,
    createEdgeForPlaceholderNode,
} from '../utils/createAiAgentEdges';
import {createClusterElementNode, createPlaceholderNode, createToolNode} from '../utils/createAiAgentNodes';

const useAiAgentLayout = () => {
    const {currentNode} = useWorkflowNodeDetailsPanelStore();
    const {setEdges, setNodes} = useAiAgentDataStore(
        useShallow((state) => ({
            setEdges: state.setEdges,
            setNodes: state.setNodes,
        }))
    );

    const canvasWidth = window.innerWidth - 120 - 460;

    const {allNodes, taskEdges} = useMemo(() => {
        const nodes: Array<Node> = [];
        const edges: Array<Edge> = [];

        if (!currentNode) return {allNodes: nodes, taskEdges: edges};

        if (currentNode) {
            const rootAiAgentNode = {
                data: currentNode,
                id: currentNode.workflowNodeName,
                position: {x: 0, y: 0},
                type: 'workflow',
            };

            nodes.push(rootAiAgentNode);
        }

        const finalToolPlaceholderNode = {
            data: {label: '+'},
            id: FINAL_PLACEHOLDER_NODE_ID,
            position: {x: 0, y: 0},
            type: 'placeholder',
        };

        const clusterElements = currentNode?.clusterElements || {
            rag: null,
            // eslint-disable-next-line sort-keys
            chatMemory: null,
            model: null,
            tools: [],
        };

        SORTED_CLUSTER_ELEMENTS_KEYS.forEach((clusterElementType) => {
            const clusterElementData = clusterElements[clusterElementType as keyof typeof clusterElements];

            if (clusterElementData === null || (Array.isArray(clusterElementData) && clusterElementData.length === 0)) {
                nodes.push(createPlaceholderNode(currentNode, clusterElementType));
            } else if (clusterElementType === 'tools' && Array.isArray(clusterElementData)) {
                clusterElementData?.forEach((tool) => {
                    nodes.push(createToolNode(tool));
                });
            } else if (clusterElementData && !Array.isArray(clusterElementData)) {
                nodes.push(createClusterElementNode(clusterElementData));
            }
        });

        const groupedClusterElementsTools = nodes.filter((node) => node.data.clusterElementType === 'tools');

        if (groupedClusterElementsTools.length > 0) {
            nodes.push(finalToolPlaceholderNode);
        }

        nodes.forEach((node) => {
            if (node.data.componentName === 'aiAgent') {
                SORTED_CLUSTER_ELEMENTS_KEYS.forEach((clusterElementType) => {
                    const clusterElementData = clusterElements[clusterElementType as keyof typeof clusterElements];
                    const targetNode = nodes.find((node) => node.data.clusterElementType === clusterElementType);

                    if (
                        clusterElementData === null ||
                        (Array.isArray(clusterElementData) && clusterElementData.length === 0)
                    ) {
                        edges.push(createEdgeForPlaceholderNode(node, clusterElementType));
                    } else if (targetNode) {
                        edges.push(createEdgeForClusterElementNode(node, targetNode));
                    }
                });
            }

            if (node.data.clusterElementType === 'tools') {
                const currentToolNode = groupedClusterElementsTools.findIndex((toolNode) => toolNode.id === node.id);
                const nextToolNodeId = groupedClusterElementsTools[currentToolNode + 1]?.id;

                if (nextToolNodeId) {
                    edges.push(createEdgeForNextToolNode(node, nextToolNodeId));
                } else {
                    edges.push(createEdgeForFinalToolPlaceholderNode(node, finalToolPlaceholderNode));
                }
            }
        });

        return {allNodes: nodes, taskEdges: edges};
    }, [currentNode]);

    useEffect(() => {
        const layoutNodes = allNodes;
        const edges: Edge[] = taskEdges;

        const elements = getLayoutedElements(layoutNodes, edges, canvasWidth);

        setNodes(elements.nodes);
        setEdges(elements.edges);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [canvasWidth, currentNode]);
};

export default useAiAgentLayout;
