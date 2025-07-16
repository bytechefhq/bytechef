import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {Edge, Node} from '@xyflow/react';

import {
    createEdgeForClusterElementNode,
    createEdgeForMultipleClusterElementNode,
    createEdgeForPlaceholderNode,
} from './clusterElementsEdgesUtils';
import {convertNameToCamelCase} from './clusterElementsUtils';

interface CreateClusterElementsEdgesProps {
    clusterRootComponentDefinition: ComponentDefinition;
    clusterRootId: string;
    nestedClusterRootsDefinitions: Record<string, ComponentDefinition>;
    nodes: Node[];
}

export default function createClusterElementsEdges({
    clusterRootComponentDefinition,
    clusterRootId,
    nestedClusterRootsDefinitions = {},
    nodes,
}: CreateClusterElementsEdgesProps) {
    const createdEdges: Edge[] = [];

    if (!clusterRootComponentDefinition?.clusterElementTypes) {
        return createdEdges;
    }

    // Find the direct children of this parent node
    const childNodes = nodes.filter(
        (node) =>
            (node.id !== clusterRootId && node.id.split('-')[0] === clusterRootId) ||
            node.data.parentClusterRootId === clusterRootId
    );

    // Get all multiple element nodes that are children of this parent
    const multipleElementNodes = childNodes.filter(
        (node) => node.data.multipleClusterElementsNode && node.type === 'workflow'
    );

    // Process each element type in the definition
    clusterRootComponentDefinition.clusterElementTypes.forEach((clusterElementType) => {
        const elementType = convertNameToCamelCase(clusterElementType.name || '');
        const isMultipleElementsNode = clusterElementType.multipleElements;

        const targetNodes = childNodes.filter((node) => node.data.clusterElementType === elementType);

        // Create edges
        if (isMultipleElementsNode) {
            const placeholderNode = targetNodes.find(
                (node) => node.type === 'placeholder' && node.data.clusterElementType === elementType
            );
            if (placeholderNode) {
                createdEdges.push(createEdgeForPlaceholderNode(clusterRootId, elementType));
            }

            const relevantMultipleNodes = multipleElementNodes.filter(
                (node) => node.data.clusterElementType === elementType
            );

            relevantMultipleNodes.forEach((node) => {
                createdEdges.push(createEdgeForMultipleClusterElementNode(clusterRootId, node));

                // For nested roots
                if (node.data.clusterElements) {
                    const componentName = (node.data.type as string)?.split('/')[0];
                    const nestedDefinition = nestedClusterRootsDefinitions[componentName];

                    if (nestedDefinition && node.data.clusterElements) {
                        const nestedEdges = createClusterElementsEdges({
                            clusterRootComponentDefinition: nestedDefinition,
                            clusterRootId: node.id,
                            nestedClusterRootsDefinitions,
                            nodes,
                        });

                        createdEdges.push(...nestedEdges);
                    }
                }
            });
        } else {
            const placeholderNode = targetNodes.find(
                (node) => node.type === 'placeholder' && node.data.clusterElementType === elementType
            );

            if (placeholderNode) {
                createdEdges.push(createEdgeForPlaceholderNode(clusterRootId, elementType));
            } else {
                const singleNode = targetNodes.find((node) => !node.data.placeholder);

                if (singleNode) {
                    createdEdges.push(createEdgeForClusterElementNode(clusterRootId, singleNode));

                    // For nested roots
                    if (singleNode.data.clusterElements) {
                        const componentName = (singleNode.data.type as string)?.split('/')[0];
                        const nestedDefinition = nestedClusterRootsDefinitions[componentName];

                        if (nestedDefinition && singleNode.data.clusterElements) {
                            const nestedEdges = createClusterElementsEdges({
                                clusterRootComponentDefinition: nestedDefinition,
                                clusterRootId: singleNode.id,
                                nestedClusterRootsDefinitions,
                                nodes,
                            });

                            createdEdges.push(...nestedEdges);
                        }
                    }
                }
            }
        }
    });

    return createdEdges;
}
