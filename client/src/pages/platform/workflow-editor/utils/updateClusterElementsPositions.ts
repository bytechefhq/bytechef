import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';

import {isPlainObject} from '../../cluster-element-editor/utils/clusterElementsUtils';

interface UpdateElementsWithPositionsProps {
    clusterElements: ClusterElementsType;
    movedClusterElementId: string;
    nodePositions: Record<string, {x: number; y: number}>;
}

export default function updateClusterElementsPositions({
    clusterElements,
    movedClusterElementId,
    nodePositions,
}: UpdateElementsWithPositionsProps): ClusterElementsType {
    const updatedClusterElements: ClusterElementsType = {};

    Object.entries(clusterElements).forEach(([elementKey, elementValue]) => {
        if (Array.isArray(elementValue)) {
            updatedClusterElements[elementKey] = elementValue.map((element) => {
                const isMovedElement = element.name === movedClusterElementId;

                const elementPosition = isMovedElement ? nodePositions[element.name] : undefined;

                const updatedElement: ClusterElementItemType = {
                    ...element,
                    ...(isMovedElement &&
                        elementPosition && {
                            metadata: {
                                ...element?.metadata,
                                ui: {
                                    ...element?.metadata?.ui,
                                    nodePosition: elementPosition,
                                },
                            },
                        }),
                };

                // Process nested elements if needed
                if (element.clusterElements) {
                    const updatedNestedClusterElements = updateClusterElementsPositions({
                        clusterElements: element.clusterElements,
                        movedClusterElementId,
                        nodePositions,
                    });

                    if (updatedNestedClusterElements !== element.clusterElements) {
                        updatedElement.clusterElements = updatedNestedClusterElements;
                    }
                }

                return updatedElement;
            });
        } else if (elementValue && isPlainObject(elementValue)) {
            const isMovedElement = elementValue.name === movedClusterElementId;

            const elementPosition = isMovedElement ? nodePositions[elementValue.name] : undefined;

            const updatedElement = {
                ...elementValue,
                ...(isMovedElement &&
                    elementPosition && {
                        metadata: {
                            ...elementValue?.metadata,
                            ui: {
                                ...elementValue?.metadata?.ui,
                                nodePosition: elementPosition,
                            },
                        },
                    }),
            };

            // Process nested elements if needed
            if (elementValue.clusterElements) {
                const updatedNestedClusterElements = updateClusterElementsPositions({
                    clusterElements: elementValue.clusterElements,
                    movedClusterElementId,
                    nodePositions,
                });

                if (updatedNestedClusterElements !== elementValue.clusterElements) {
                    updatedElement.clusterElements = updatedNestedClusterElements;
                }
            }

            updatedClusterElements[elementKey] = updatedElement;
        }
    });

    return updatedClusterElements;
}
