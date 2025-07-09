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
        const elementType = convertNameToCamelCase(clusterElementType.name || '');
        const elementLabel = clusterElementType.label || '';
        const isMultipleElementsNode = clusterElementType.multipleElements;
        const clusterElementData = clusterElements[elementType];

        if (isMultipleElementsNode) {
            if (Array.isArray(clusterElementData) && clusterElementData.length) {
                clusterElementData.forEach((element, multipleElementIndex) => {
                    // Create the multiple element node
                    const elementNode = createMultipleElementsNode(
                        clusterElementTypeIndex,
                        clusterRootId,
                        currentNodePositions,
                        element,
                        elementType,
                        isMultipleElementsNode,
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
                elementLabel,
                elementType,
                totalClusterElementTypeCount
            );

            createdNodes.push(placeholderNode);
        } else {
            if (clusterElementData && !Array.isArray(clusterElementData)) {
                // Create the single element node
                const elementNode = createSingleElementsNode(
                    clusterElementData,
                    clusterElementTypeIndex,
                    clusterRootId,
                    currentNodePositions,
                    elementLabel,
                    elementType,
                    totalClusterElementTypeCount
                );

                // Set root parent/child relationship
                elementNode.data.parentClusterRootId = clusterRootId;
                elementNode.data.isNestedClusterRoot = !!clusterElementData.clusterElements;

                createdNodes.push(elementNode);

                // Process nested roots
                if (clusterElementData.clusterElements) {
                    const componentName = clusterElementData.type?.split('/')[0];

                    const nestedDefinition = nestedClusterRootsDefinitions[componentName];

                    if (nestedDefinition) {
                        const childNodes = createClusterElementNodes({
                            clusterElements: clusterElementData.clusterElements,
                            clusterRootComponentDefinition: nestedDefinition,
                            clusterRootId: clusterElementData.name,
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
                    elementLabel,
                    elementType,
                    totalClusterElementTypeCount
                );

                createdNodes.push(placeholderNode);
            }
        }
    });

    return createdNodes;
}
