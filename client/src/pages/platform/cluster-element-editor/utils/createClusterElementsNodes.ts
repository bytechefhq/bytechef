import {toClusterElementArray} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
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
            // Tolerates a lone stored object as well as an array — see toClusterElementArray.
            const clusterElementValues = toClusterElementArray(clusterElementValue);

            if (clusterElementValues.length) {
                clusterElementValues.forEach((element) => {
                    const filteredClusterElementTypes = getFilteredClusterElementTypes({
                        clusterRootComponentDefinition: nestedClusterRootsDefinitions[element.type?.split('/')[0]],
                        currentClusterElementsType: element.type?.split('/')[2] || '',
                        isNestedClusterRoot: true,
                    });

                    // Nesting is decided by the element's own definition, not by the presence of a seeded
                    // clusterElements object. That object is written once, when the element is added, so an element
                    // added before its component declared child types would otherwise stay a leaf forever.
                    const isNestedClusterRoot = filteredClusterElementTypes.length > 0;

                    // Create the multiple element node
                    const multipleElementsNode = createMultipleElementsNode({
                        clusterElementTypeIndex,
                        clusterElementTypeName,
                        clusterRootId,
                        currentNestedRootElementTypesCount: isNestedClusterRoot
                            ? filteredClusterElementTypes.length
                            : undefined,
                        element,
                        isMultipleClusterElementsNode,
                        parentClusterRootElementsTypeCount,
                    });

                    // Set root parent/child relationship
                    multipleElementsNode.data.parentClusterRootId = clusterRootId;
                    multipleElementsNode.data.isNestedClusterRoot = isNestedClusterRoot;

                    createdNodes.push(multipleElementsNode);

                    // Process nested roots
                    if (isNestedClusterRoot) {
                        const componentName = element.type?.split('/')[0];

                        const nestedClusterRootDefinition = nestedClusterRootsDefinitions[componentName];

                        if (nestedClusterRootDefinition) {
                            const nestedClusterElementNodes = createClusterElementNodes({
                                clusterElements: element.clusterElements ?? {},
                                clusterRootId: element.name,
                                currentRootComponentDefinition: nestedClusterRootDefinition,
                                nestedClusterRootElementType: element.type?.split('/')[2] || clusterElementTypeName,
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
                    isNestedClusterRoot: true,
                });

                const isNestedClusterRoot = filteredClusterElementTypes.length > 0;

                // Create the single element node
                const singleElementNode = createSingleElementsNode({
                    clusterElementItem: clusterElementValue,
                    clusterElementTypeIndex,
                    clusterElementTypeLabel,
                    clusterElementTypeName,
                    clusterRootId,
                    currentNestedRootElementTypesCount: isNestedClusterRoot
                        ? filteredClusterElementTypes.length
                        : undefined,
                    parentClusterRootElementsTypeCount,
                });

                // Set root parent/child relationship
                singleElementNode.data.parentClusterRootId = clusterRootId;
                singleElementNode.data.isNestedClusterRoot = isNestedClusterRoot;

                createdNodes.push(singleElementNode);

                // Process nested roots
                if (isNestedClusterRoot) {
                    const componentName = clusterElementValue.type?.split('/')[0];

                    const nestedClusterRootDefinition = nestedClusterRootsDefinitions[componentName];

                    if (nestedClusterRootDefinition) {
                        const nestedClusterElementNodes = createClusterElementNodes({
                            clusterElements: clusterElementValue.clusterElements ?? {},
                            clusterRootId: clusterElementValue.name,
                            currentRootComponentDefinition: nestedClusterRootDefinition,
                            nestedClusterRootElementType:
                                clusterElementValue.type?.split('/')[2] || clusterElementTypeName,
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
