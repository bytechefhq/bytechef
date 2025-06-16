import {Workflow} from '@/shared/middleware/platform/configuration';
import {ClusterElementItemType, UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

import useClusterElementsDataStore from '../../cluster-element-editor/stores/useClusterElementsDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface SaveClusterElementNodesPositionProps {
    invalidateWorkflowQueries: () => void;
    queryClient: QueryClient;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
}

export default function saveClusterElementNodesPosition({
    invalidateWorkflowQueries,
    updateWorkflowMutation,
    workflow,
}: SaveClusterElementNodesPositionProps) {
    const {nodes: clusterElementNodes} = useClusterElementsDataStore.getState();
    const {rootClusterElementNodeData, setRootClusterElementNodeData} = useWorkflowEditorStore.getState();
    const {currentNode, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();

    if (!workflow.definition || !rootClusterElementNodeData) {
        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

    const currentClusterRootTask = workflowDefinitionTasks.find(
        (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
    );

    if (!currentClusterRootTask || !currentClusterRootTask.clusterElements) {
        return;
    }

    const clusterElements: Record<string, ClusterElementItemType | ClusterElementItemType[]> =
        currentClusterRootTask.clusterElements;

    const nodePositions = clusterElementNodes.reduce<Record<string, {x: number; y: number}>>((accumulator, node) => {
        accumulator[node.id] = {
            x: node.position.x,
            y: node.position.y,
        };

        return accumulator;
    }, {});

    Object.entries(clusterElements).forEach(([elementKey, elementValue]) => {
        if (Array.isArray(elementValue)) {
            clusterElements[elementKey] = elementValue.map((element) => {
                const elementNodeId = element.name;

                const elementPosition = nodePositions[elementNodeId];

                if (elementPosition) {
                    return {
                        ...element,
                        metadata: {
                            ...element?.metadata,
                            ui: {
                                ...element?.metadata?.ui,
                                nodePosition: elementPosition,
                            },
                        },
                    };
                }

                return element;
            });
        } else if (elementValue != null && 'name' in elementValue) {
            const elementNodeId = elementValue.name;
            const elementPosition = nodePositions[elementNodeId];

            if (elementPosition) {
                clusterElements[elementKey] = {
                    ...elementValue,
                    metadata: {
                        ...elementValue?.metadata,
                        ui: {
                            ...elementValue?.metadata?.ui,
                            nodePosition: elementPosition,
                        },
                    },
                } as ClusterElementItemType;
            }
        }
    });

    const placeholderPositions = Object.entries(nodePositions).reduce<Record<string, {x: number; y: number}>>(
        (accumulator, [nodeId, position]) => {
            if (nodeId.includes('placeholder')) {
                accumulator[nodeId] = position;
            }

            return accumulator;
        },
        {}
    );

    const rootNodePosition = rootClusterElementNodeData?.workflowNodeName
        ? nodePositions[rootClusterElementNodeData.workflowNodeName]
        : undefined;

    const metadata = {
        ...currentClusterRootTask.metadata,
        ui: {
            ...currentClusterRootTask.metadata?.ui,
            nodePosition: rootNodePosition,
            placeholderPositions: placeholderPositions || {},
        },
    };

    const updatedNodeData = {
        ...currentClusterRootTask,
        clusterElements,
        metadata,
    };

    setRootClusterElementNodeData({
        ...rootClusterElementNodeData,
        clusterElements,
    } as typeof rootClusterElementNodeData);

    if (currentNode?.rootClusterElement) {
        setCurrentNode({
            ...currentNode,
            clusterElements,
        });
    }

    saveWorkflowDefinition({
        invalidateWorkflowQueries,
        nodeData: updatedNodeData,
        updateWorkflowMutation,
    });
}
