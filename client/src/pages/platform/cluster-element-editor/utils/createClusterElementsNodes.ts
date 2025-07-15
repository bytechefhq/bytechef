import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementsType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {createMultipleElementsNode, createPlaceholderNode, createSingleElementsNode} from './clusterElementsNodesUtils';
import {convertNameToCamelCase} from './clusterElementsUtils';

interface CreateClusterElementNodesProps {
    clusterElements: ClusterElementsType;
    clusterRootComponentDefinition: ComponentDefinition;
    clusterRootId: string;
    currentNodePositions?: Record<string, {x: number; y: number}>;
    nestedClusterRootsDefinitions: Record<string, ComponentDefinition>;
}

export default function createClusterElementNodes({
    clusterElements,
    clusterRootComponentDefinition,
    clusterRootId,
    currentNodePositions,
    nestedClusterRootsDefinitions,
}: CreateClusterElementNodesProps) {
    if (!clusterRootComponentDefinition?.clusterElementTypes || !clusterElements) {
        return [];
    }

    const createdNodes: Node[] = [];
    const totalClusterElementTypeCount = clusterRootComponentDefinition.clusterElementTypes.length;

    clusterRootComponentDefinition.clusterElementTypes.forEach((clusterElementType, clusterElementTypeIndex) => {
        const clusterElementTypeName = convertNameToCamelCase(clusterElementType.name || '');
        const clusterElementTypeLabel = clusterElementType.label || '';
        const isMultipleClusterElementsNode = clusterElementType.multipleElements;
        const clusterElementValue = clusterElements[clusterElementTypeName];

        if (isMultipleClusterElementsNode) {
            if (Array.isArray(clusterElementValue) && clusterElementValue.length) {
                clusterElementValue.forEach((element, multipleElementIndex) => {
                    // Create the multiple element node
                    const elementNode = createMultipleElementsNode(
                        clusterElementTypeIndex,
                        clusterRootId,
                        currentNodePositions,
                        element,
                        clusterElementTypeName,
                        isMultipleClusterElementsNode,
                        multipleElementIndex,
                        totalClusterElementTypeCount
                    );

                    // Set root parent/child relationship
                    elementNode.data.parentClusterRootId = clusterRootId;
                    elementNode.data.isNestedClusterRoot = !!element.clusterElements;

                    createdNodes.push(elementNode);

                    // Process nested roots
                    if (element.clusterElements) {
                        const componentName = element.type?.split('/')[0];

                        const nestedDefinition = nestedClusterRootsDefinitions[componentName];

                        if (nestedDefinition) {
                            const childNodes = createClusterElementNodes({
                                clusterElements: element.clusterElements,
                                clusterRootComponentDefinition: nestedDefinition,
                                clusterRootId: element.name,
                                currentNodePositions,
                                nestedClusterRootsDefinitions,
                            });

                            createdNodes.push(...childNodes);
                        }
                    }
                });
            }

            // Always add placeholders for multiple elements nodes
            const placeholderNode = createPlaceholderNode(
                clusterElementTypeIndex,
                clusterRootId,
                currentNodePositions,
                clusterElementTypeLabel,
                clusterElementTypeName,
                totalClusterElementTypeCount
            );

            createdNodes.push(placeholderNode);
        } else {
            if (clusterElementValue && !Array.isArray(clusterElementValue)) {
                // Create the single element node
                const elementNode = createSingleElementsNode(
                    clusterElementValue,
                    clusterElementTypeIndex,
                    clusterRootId,
                    currentNodePositions,
                    clusterElementTypeLabel,
                    clusterElementTypeName,
                    totalClusterElementTypeCount
                );

                // Set root parent/child relationship
                elementNode.data.parentClusterRootId = clusterRootId;
                elementNode.data.isNestedClusterRoot = !!clusterElementValue.clusterElements;

                createdNodes.push(elementNode);

                // Process nested roots
                if (clusterElementValue.clusterElements) {
                    const componentName = clusterElementValue.type?.split('/')[0];

                    const nestedDefinition = nestedClusterRootsDefinitions[componentName];

                    if (nestedDefinition) {
                        const childNodes = createClusterElementNodes({
                            clusterElements: clusterElementValue.clusterElements,
                            clusterRootComponentDefinition: nestedDefinition,
                            clusterRootId: clusterElementValue.name,
                            currentNodePositions,
                            nestedClusterRootsDefinitions,
                        });

                        createdNodes.push(...childNodes);
                    }
                }
            } else {
                const placeholderNode = createPlaceholderNode(
                    clusterElementTypeIndex,
                    clusterRootId,
                    currentNodePositions,
                    clusterElementTypeLabel,
                    clusterElementTypeName,
                    totalClusterElementTypeCount
                );

                createdNodes.push(placeholderNode);
            }
        }
    });

    return createdNodes;
}
