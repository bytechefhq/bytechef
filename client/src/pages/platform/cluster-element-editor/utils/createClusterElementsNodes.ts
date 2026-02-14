import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementsType, NestedClusterRootComponentDefinitionType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {createMultipleElementsNode, createPlaceholderNode, createSingleElementsNode} from './clusterElementsNodesUtils';
import {convertNameToCamelCase, getFilteredClusterElementTypes, isPlainObject} from './clusterElementsUtils';

interface CreateClusterElementNodesProps {
    clusterElements: ClusterElementsType;
    clusterRootId: string;
    currentRootComponentDefinition: ComponentDefinition | NestedClusterRootComponentDefinitionType;
    nestedClusterRootElementType?: string;
    nestedClusterRootsDefinitions: Record<string, NestedClusterRootComponentDefinitionType>;
    operationName?: string;
}

export default function createClusterElementNodes({
    clusterElements,
    clusterRootId,
    currentRootComponentDefinition,
    nestedClusterRootElementType,
    nestedClusterRootsDefinitions,
    operationName = '',
}: CreateClusterElementNodesProps) {
    if (!currentRootComponentDefinition || !currentRootComponentDefinition.clusterElementTypes || !clusterElements) {
        return [];
    }

    const createdNodes: Node[] = [];

    const nestedClusterRootRequirementMet =
        nestedClusterRootElementType &&
        nestedClusterRootsDefinitions &&
        Object.keys(nestedClusterRootsDefinitions).length > 0;

    const filteredClusterElementTypes = getFilteredClusterElementTypes({
        clusterRootComponentDefinition: currentRootComponentDefinition,
        currentClusterElementsType: nestedClusterRootElementType,
        isNestedClusterRoot: !!nestedClusterRootRequirementMet,
        operationName,
    });

    const parentClusterRootElementsTypeCount = filteredClusterElementTypes.length;

    filteredClusterElementTypes.forEach((clusterElementType, clusterElementTypeIndex) => {
        const clusterElementTypeName = convertNameToCamelCase(clusterElementType.name || '');
        const clusterElementTypeLabel = clusterElementType.label || '';
        const isMultipleClusterElementsNode = clusterElementType.multipleElements;
        const clusterElementValue = clusterElements[clusterElementTypeName];

        if (isMultipleClusterElementsNode) {
            if (Array.isArray(clusterElementValue) && clusterElementValue.length) {
                clusterElementValue.forEach((element) => {
                    const filteredClusterElementTypes = getFilteredClusterElementTypes({
                        clusterRootComponentDefinition: nestedClusterRootsDefinitions[element.type?.split('/')[0]],
                        currentClusterElementsType: element.type?.split('/')[2] || '',
                        isNestedClusterRoot: !!element.clusterElements,
                    });

                    // Create the multiple element node
                    const multipleElementsNode = createMultipleElementsNode({
                        clusterElementTypeIndex,
                        clusterElementTypeName,
                        clusterRootId,
                        currentNestedRootElementTypesCount: element.clusterElements
                            ? filteredClusterElementTypes.length
                            : undefined,
                        element,
                        isMultipleClusterElementsNode,
                        parentClusterRootElementsTypeCount,
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
                                clusterRootId: element.name,
                                currentRootComponentDefinition: nestedClusterRootDefinition,
                                nestedClusterRootElementType: clusterElementTypeName,
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
                isMultipleClusterElementsNode,
                parentClusterRootElementsTypeCount,
            });

            createdNodes.push(placeholderNode);
        } else {
            if (clusterElementValue && isPlainObject(clusterElementValue)) {
                const filteredClusterElementTypes = getFilteredClusterElementTypes({
                    clusterRootComponentDefinition:
                        nestedClusterRootsDefinitions[clusterElementValue.type?.split('/')[0]],
                    currentClusterElementsType: clusterElementValue.type?.split('/')[2] || '',
                    isNestedClusterRoot: !!clusterElementValue.clusterElements,
                });

                // Create the single element node
                const singleElementNode = createSingleElementsNode({
                    clusterElementItem: clusterElementValue,
                    clusterElementTypeIndex,
                    clusterElementTypeLabel,
                    clusterElementTypeName,
                    clusterRootId,
                    currentNestedRootElementTypesCount: clusterElementValue.clusterElements
                        ? filteredClusterElementTypes.length
                        : undefined,
                    parentClusterRootElementsTypeCount,
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
                            clusterRootId: clusterElementValue.name,
                            currentRootComponentDefinition: nestedClusterRootDefinition,
                            nestedClusterRootElementType: clusterElementTypeName,
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
                    parentClusterRootElementsTypeCount,
                });

                createdNodes.push(placeholderNode);
            }
        }
    });

    return createdNodes;
}
