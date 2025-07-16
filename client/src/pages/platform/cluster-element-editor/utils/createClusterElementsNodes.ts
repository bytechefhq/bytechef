import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementsType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {createMultipleElementsNode, createPlaceholderNode, createSingleElementsNode} from './clusterElementsNodesUtils';
import {convertNameToCamelCase} from './clusterElementsUtils';

interface CreateClusterElementNodesProps {
    clusterElements: ClusterElementsType;
    clusterRootComponentDefinition: ComponentDefinition;
    clusterRootId: string;
    currentNodePositions: Record<string, {x: number; y: number}>;
    nestedClusterRootsDefinitions: Record<string, ComponentDefinition>;
}

export default function createClusterElementNodes({
    clusterElements,
    clusterRootComponentDefinition,
    clusterRootId,
    currentNodePositions = {},
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
                    const multipleElementsNode = createMultipleElementsNode({
                        clusterElementTypeIndex,
                        clusterElementTypeName,
                        clusterRootId,
                        currentNodePositions,
                        element,
                        isMultipleClusterElementsNode,
                        multipleElementIndex,
                        totalClusterElementTypeCount,
                    });

                    // Set root parent/child relationship
                    multipleElementsNode.data.parentClusterRootId = clusterRootId;
                    multipleElementsNode.data.isNestedClusterRoot = !!element.clusterElements;

                    createdNodes.push(multipleElementsNode);

                    // Process nested roots
                    if (element.clusterElements) {
                        const componentName = element.type?.split('/')[0];

                        const nestedClusterRootDefinition = nestedClusterRootsDefinitions[componentName];

                        if (nestedClusterRootDefinition) {
                            const nestedClusterElementNodes = createClusterElementNodes({
                                clusterElements: element.clusterElements,
                                clusterRootComponentDefinition: nestedClusterRootDefinition,
                                clusterRootId: element.name,
                                currentNodePositions,
                                nestedClusterRootsDefinitions,
                            });

                            createdNodes.push(...nestedClusterElementNodes);
                        }
                    }
                });
            }

            // Always add placeholders for multiple elements nodes
            const placeholderNode = createPlaceholderNode({
                clusterElementTypeIndex,
                clusterElementTypeLabel,
                clusterElementTypeName,
                clusterRootId,
                currentNodePositions,
                isMultipleClusterElementsNode,
                totalClusterElementTypeCount,
            });

            createdNodes.push(placeholderNode);
        } else {
            if (clusterElementValue && !Array.isArray(clusterElementValue)) {
                // Create the single element node
                const singleElementNode = createSingleElementsNode({
                    clusterElementItem: clusterElementValue,
                    clusterElementTypeIndex,
                    clusterElementTypeLabel,
                    clusterElementTypeName,
                    clusterRootId,
                    currentNodePositions,
                    totalClusterElementTypeCount,
                });

                // Set root parent/child relationship
                singleElementNode.data.parentClusterRootId = clusterRootId;
                singleElementNode.data.isNestedClusterRoot = !!clusterElementValue.clusterElements;

                createdNodes.push(singleElementNode);

                // Process nested roots
                if (clusterElementValue.clusterElements) {
                    const componentName = clusterElementValue.type?.split('/')[0];

                    const nestedClusterRootDefinition = nestedClusterRootsDefinitions[componentName];

                    if (nestedClusterRootDefinition) {
                        const nestedClusterElementNodes = createClusterElementNodes({
                            clusterElements: clusterElementValue.clusterElements,
                            clusterRootComponentDefinition: nestedClusterRootDefinition,
                            clusterRootId: clusterElementValue.name,
                            currentNodePositions,
                            nestedClusterRootsDefinitions,
                        });

                        createdNodes.push(...nestedClusterElementNodes);
                    }
                }
            } else {
                const placeholderNode = createPlaceholderNode({
                    clusterElementTypeIndex,
                    clusterElementTypeLabel,
                    clusterElementTypeName,
                    clusterRootId,
                    currentNodePositions,
                    totalClusterElementTypeCount,
                });

                createdNodes.push(placeholderNode);
            }
        }
    });

    return createdNodes;
}
