import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementsType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {createMultipleElementsNode, createPlaceholderNode, createSingleElementsNode} from './clusterElementsNodesUtils';
import {convertNameToCamelCase, getClusterElementTypesCount, isPlainObject} from './clusterElementsUtils';

interface CreateClusterElementNodesProps {
    clusterElements: ClusterElementsType;
    currentRootComponentDefinition: ComponentDefinition;
    clusterRootId: string;
    nestedClusterRootsDefinitions: Record<string, ComponentDefinition>;
    operationName?: string;
}

export default function createClusterElementNodes({
    clusterElements,
    clusterRootId,
    currentRootComponentDefinition,
    nestedClusterRootsDefinitions,
    operationName = '',
}: CreateClusterElementNodesProps) {
    if (!currentRootComponentDefinition || !currentRootComponentDefinition.clusterElementTypes || !clusterElements) {
        return [];
    }

    const createdNodes: Node[] = [];

    /**
     * If current cluster root component has actionClusterElementTypes object in its definition,
     * filter cluster element types based on the action (operationName) chosen by the user in the operations popover menu,
     * if not then return all cluster element types from definition
     */
    const filteredClusterElementTypes = currentRootComponentDefinition.clusterElementTypes.filter((elementType) => {
        if (!operationName) {
            return true;
        }

        const actionTypes = currentRootComponentDefinition.actionClusterElementTypes;

        if (!actionTypes || Object.keys(actionTypes).length === 0) {
            return true;
        }

        const operationElementTypes = actionTypes[operationName];

        if (!operationElementTypes || operationElementTypes.length === 0) {
            return true;
        }

        return operationElementTypes.includes(elementType.name || '');
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
                    const clusterElementTypesCount = getClusterElementTypesCount({
                        clusterRootComponentDefinition: nestedClusterRootsDefinitions[element.type?.split('/')[0]],
                    });

                    // Create the multiple element node
                    const multipleElementsNode = createMultipleElementsNode({
                        clusterElementTypeName,
                        clusterRootId,
                        currentNestedRootElementTypesCount: element.clusterElements
                            ? clusterElementTypesCount
                            : undefined,
                        element,
                        isMultipleClusterElementsNode,
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
                const clusterElementTypesCount = getClusterElementTypesCount({
                    clusterRootComponentDefinition:
                        nestedClusterRootsDefinitions[clusterElementValue.type?.split('/')[0]],
                });

                // Create the single element node
                const singleElementNode = createSingleElementsNode({
                    clusterElementItem: clusterElementValue,
                    clusterElementTypeLabel,
                    clusterElementTypeName,
                    clusterRootId,
                    currentNestedRootElementTypesCount: clusterElementValue.clusterElements
                        ? clusterElementTypesCount
                        : undefined,
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
