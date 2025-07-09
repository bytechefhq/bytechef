import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';

import updateClusterElementsPositions from './updateClusterElementsPositions';

interface ProcessClusterElementsHierarchyProps {
    clusterElementData: ClusterElementItemType;
    clusterElements: ClusterElementsType;
    elementType: string;
    isMultipleElements: boolean;
    mainRootId?: string;
    nodePositions: Record<string, {x: number; y: number}>;
    sourceNodeId: string;
}

export default function processClusterElementsHierarchy({
    clusterElementData,
    clusterElements,
    elementType,
    isMultipleElements = false,
    mainRootId,
    nodePositions,
    sourceNodeId,
}: ProcessClusterElementsHierarchyProps): {parentFound: boolean; nestedClusterElements: ClusterElementsType} {
    let updatedClusterElements = {...clusterElements};
    let parentFound = false;

    if (!sourceNodeId || !clusterElementData || elementType === undefined) {
        updatedClusterElements = updateClusterElementsPositions({
            clusterElements: updatedClusterElements,
            nodePositions,
        });

        return {nestedClusterElements: updatedClusterElements, parentFound};
    }

    if (mainRootId && sourceNodeId === mainRootId) {
        // Add element to the main root
        if (isMultipleElements) {
            updatedClusterElements[elementType] = [
                ...(Array.isArray(updatedClusterElements[elementType]) ? updatedClusterElements[elementType] : []),
                clusterElementData,
            ];
        } else {
            updatedClusterElements[elementType] = clusterElementData;
        }

        parentFound = true;
    } else {
        // Process the hierarchy to find the parent node and add the element
        Object.values(updatedClusterElements).forEach((value) => {
            if (parentFound) {
                return;
            }

            if (Array.isArray(value)) {
                value.forEach((element) => {
                    if (parentFound) {
                        return;
                    }

                    // Check if this is the correct root
                    if (element.name === sourceNodeId) {
                        if (!element.clusterElements) {
                            element.clusterElements = {};
                        }

                        // Add element to this root
                        if (isMultipleElements) {
                            element.clusterElements[elementType] = [
                                ...(Array.isArray(element.clusterElements[elementType])
                                    ? element.clusterElements[elementType]
                                    : []),
                                clusterElementData,
                            ];
                        } else {
                            element.clusterElements[elementType] = clusterElementData;
                        }

                        parentFound = true;

                        return;
                    }
                    // Process nested cluster elements
                    if (element.clusterElements) {
                        const result = processClusterElementsHierarchy({
                            clusterElementData,
                            clusterElements: element.clusterElements,
                            elementType,
                            isMultipleElements,
                            nodePositions,
                            sourceNodeId,
                        });

                        if (result.parentFound) {
                            element.clusterElements = result.nestedClusterElements;

                            parentFound = true;

                            return;
                        }
                    }
                });
            } else if (value && typeof value === 'object') {
                // Check if this is the correct root
                if (value.name === sourceNodeId) {
                    if (!value.clusterElements) {
                        value.clusterElements = {};
                    }

                    // Add element to this root
                    if (isMultipleElements) {
                        value.clusterElements[elementType] = [
                            ...(Array.isArray(value.clusterElements[elementType])
                                ? value.clusterElements[elementType]
                                : []),
                            clusterElementData,
                        ];
                    } else {
                        value.clusterElements[elementType] = clusterElementData;
                    }

                    parentFound = true;

                    return;
                }

                // Process nested cluster elements
                if (value.clusterElements) {
                    const result = processClusterElementsHierarchy({
                        clusterElementData,
                        clusterElements: value.clusterElements,
                        elementType,
                        isMultipleElements,
                        nodePositions,
                        sourceNodeId,
                    });

                    if (result.parentFound) {
                        value.clusterElements = result.nestedClusterElements;

                        parentFound = true;

                        return;
                    }
                }
            }
        });
    }

    updatedClusterElements = updateClusterElementsPositions({
        clusterElements: updatedClusterElements,
        nodePositions,
    });

    return {nestedClusterElements: updatedClusterElements, parentFound};
}
