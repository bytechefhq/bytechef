import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';

import {addElementToClusterRoot, isPlainObject} from '../../cluster-element-editor/utils/clusterElementsUtils';

interface ProcessClusterElementsHierarchyProps {
    clusterElementData: ClusterElementItemType;
    clusterElements: ClusterElementsType;
    elementType: string;
    isMultipleElements: boolean;
    mainRootId?: string;
    sourceNodeId: string;
}

export default function processClusterElementsHierarchy({
    clusterElementData,
    clusterElements,
    elementType,
    isMultipleElements = false,
    mainRootId,
    sourceNodeId,
}: ProcessClusterElementsHierarchyProps): {parentFound: boolean; nestedClusterElements: ClusterElementsType} {
    let parentFound = false;

    if (!sourceNodeId || !clusterElementData || elementType === undefined) {
        return {
            nestedClusterElements: clusterElements,
            parentFound: false,
        };
    }

    // Add element to the main root
    if (mainRootId && sourceNodeId === mainRootId) {
        return {
            nestedClusterElements: addElementToClusterRoot({
                clusterElementTypeLabel: elementType,
                clusterElementValue: clusterElementData,
                clusterElements,
                isMultipleElements,
            }),

            parentFound: true,
        };
    }

    // Process the hierarchy to find the parent node and add the element
    const updatedClusterElements = Object.fromEntries(
        Object.entries(clusterElements).map(([clusterElementType, clusterElementValue]) => {
            if (parentFound) {
                return [clusterElementType, clusterElementValue];
            }

            if (Array.isArray(clusterElementValue)) {
                const updatedElements = clusterElementValue.map((element) => {
                    if (parentFound) {
                        return element;
                    }

                    // Check if this is the correct root and add the element
                    if (element.name === sourceNodeId && element.clusterElements) {
                        parentFound = true;

                        return {
                            ...element,
                            clusterElements: addElementToClusterRoot({
                                clusterElementTypeLabel: elementType,
                                clusterElementValue: clusterElementData,
                                clusterElements: element.clusterElements || {},
                                isMultipleElements,
                            }),
                        };
                    } else if (element.clusterElements) {
                        // Process nested cluster elements
                        const updatedElements = processClusterElementsHierarchy({
                            clusterElementData,
                            clusterElements: element.clusterElements,
                            elementType,
                            isMultipleElements,
                            mainRootId,
                            sourceNodeId,
                        });

                        if (updatedElements.parentFound) {
                            parentFound = true;

                            return {...element, clusterElements: updatedElements.nestedClusterElements};
                        }
                    }

                    return element;
                });

                return [clusterElementType, updatedElements];
            } else if (isPlainObject(clusterElementValue)) {
                if (parentFound) {
                    return [clusterElementType, clusterElementValue];
                }

                // Check if this is the correct root and add the element
                if (clusterElementValue.name === sourceNodeId) {
                    parentFound = true;

                    return [
                        clusterElementType,
                        {
                            ...clusterElementValue,
                            clusterElements: addElementToClusterRoot({
                                clusterElementTypeLabel: elementType,
                                clusterElementValue: clusterElementData,
                                clusterElements: clusterElementValue.clusterElements || {},
                                isMultipleElements,
                            }),
                        },
                    ];
                } else if (clusterElementValue.clusterElements) {
                    // Process nested cluster elements
                    const updatedElements = processClusterElementsHierarchy({
                        clusterElementData,
                        clusterElements: clusterElementValue.clusterElements,
                        elementType,
                        isMultipleElements,
                        mainRootId,
                        sourceNodeId,
                    });

                    if (updatedElements.parentFound) {
                        parentFound = true;

                        return [
                            clusterElementType,
                            {...clusterElementValue, clusterElements: updatedElements.nestedClusterElements},
                        ];
                    }
                }

                return [clusterElementType, clusterElementValue];
            }

            return [clusterElementType, clusterElementValue];
        })
    );

    return {
        nestedClusterElements: updatedClusterElements,
        parentFound,
    };
}
