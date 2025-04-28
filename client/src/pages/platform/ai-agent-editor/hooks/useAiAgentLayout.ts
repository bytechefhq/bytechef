import {SORTED_CLUSTER_ELEMENTS_KEYS} from '@/shared/constants';
import {Edge, Node} from '@xyflow/react';
import {useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowEditorStore from '../../workflow-editor/stores/useWorkflowEditorStore';
import {getLayoutedElements} from '../../workflow-editor/utils/layoutUtils';
import useAiAgentDataStore from '../stores/useAiAgentDataStore';
import {
    createEdgeForClusterElementNode,
    createEdgeForPlaceholderNode,
    createEdgeForToolNode,
    createEdgeForToolsGhostNode,
    createEdgeForToolsPlaceholderNode,
} from '../utils/createAiAgentEdges';
import {
    createClusterElementNode,
    createPlaceholderNode,
    createToolNode,
    createToolsGhostNode,
} from '../utils/createAiAgentNodes';

const useAiAgentLayout = () => {
    const {aiAgentNodeData} = useWorkflowEditorStore();

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

        if (!aiAgentNodeData) {
            return {allNodes: nodes, taskEdges: edges};
        }

        if (aiAgentNodeData) {
            const rootAiAgentNode = {
                data: aiAgentNodeData,
                id: aiAgentNodeData.workflowNodeName,
                position: {x: 0, y: 0},
                type: 'workflow',
            };

            nodes.push(rootAiAgentNode);
        }

        const clusterElements = aiAgentNodeData.clusterElements || {
            rag: null,
            // eslint-disable-next-line sort-keys
            chatMemory: null,
            model: null,
            tools: [],
        };

        if (clusterElements) {
            SORTED_CLUSTER_ELEMENTS_KEYS.forEach((clusterElementType) => {
                const clusterElementData = clusterElements[clusterElementType as keyof typeof clusterElements];
                const currentAiAgentNodeName = aiAgentNodeData.workflowNodeName;

                if (clusterElementType === 'tools') {
                    nodes.push(createToolsGhostNode(currentAiAgentNodeName));

                    if (Array.isArray(clusterElementData) && clusterElementData.length) {
                        clusterElementData.forEach((tool) => {
                            nodes.push(createToolNode(tool));
                        });
                    }

                    nodes.push(createPlaceholderNode(currentAiAgentNodeName, clusterElementType));
                } else {
                    if (clusterElementData && !Array.isArray(clusterElementData)) {
                        nodes.push(createClusterElementNode(clusterElementData));
                    } else {
                        nodes.push(createPlaceholderNode(currentAiAgentNodeName, clusterElementType));
                    }
                }
            });
        }

        const toolNodes = nodes.filter((node) => node.data.clusterElementType === 'tools' && node.type === 'workflow');

        nodes.forEach((node) => {
            if (node.data.componentName === 'aiAgent') {
                SORTED_CLUSTER_ELEMENTS_KEYS.forEach((clusterElementType) => {
                    const clusterElementData = clusterElements[clusterElementType as keyof typeof clusterElements];
                    const nodeId = node.id;

                    if (clusterElementType === 'tools') {
                        const edgeFromAiAgentToToolsGhostNode = createEdgeForToolsGhostNode(nodeId);

                        edges.push(edgeFromAiAgentToToolsGhostNode);
                    } else {
                        const targetNode = nodes.find((node) => node.data.clusterElementType === clusterElementType);

                        if (clusterElementData && !Array.isArray(clusterElementData) && targetNode) {
                            const edgeFromAiAgentToClusterElementNode = createEdgeForClusterElementNode(
                                nodeId,
                                targetNode
                            );

                            edges.push(edgeFromAiAgentToClusterElementNode);
                        } else {
                            const edgeFromAiAgentToPlaceholderNode = createEdgeForPlaceholderNode(
                                nodeId,
                                clusterElementType
                            );

                            edges.push(edgeFromAiAgentToPlaceholderNode);
                        }
                    }
                });
            }

            if (node.type === 'aiAgentToolsGhostNode') {
                const currentNodeId = node.id;
                const aiAgentId = node.data.aiAgentId as string;

                if (toolNodes.length) {
                    toolNodes.forEach((toolNode) => {
                        const toolNodeId = toolNode.id;

                        const edgeFromToolsGhostNodeToToolNode = createEdgeForToolNode(
                            aiAgentId,
                            currentNodeId,
                            toolNodeId
                        );

                        edges.push(edgeFromToolsGhostNodeToToolNode);
                    });
                }

                const edgeFromToolsGhostNodeToToolsPlaceholderNode = createEdgeForToolsPlaceholderNode(
                    currentNodeId,
                    aiAgentId
                );

                edges.push(edgeFromToolsGhostNodeToToolsPlaceholderNode);
            }
        });

        return {allNodes: nodes, taskEdges: edges};
    }, [aiAgentNodeData]);

    useEffect(() => {
        const layoutNodes = allNodes;
        const edges: Edge[] = taskEdges;

        const elements = getLayoutedElements({canvasWidth, edges, isAiAgentCanvas: true, nodes: layoutNodes});

        setNodes(elements.nodes);
        setEdges(elements.edges);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [canvasWidth, aiAgentNodeData]);
};

export default useAiAgentLayout;
