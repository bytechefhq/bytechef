import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';

interface UpdateElementsWithPositionsProps {
    clusterElements: ClusterElementsType;
    nodePositions: Record<string, {x: number; y: number}>;
}

export default function updateClusterElementsPositions({
    clusterElements,
    nodePositions,
}: UpdateElementsWithPositionsProps): ClusterElementsType {
    const updatedElements = {...clusterElements};

    Object.entries(updatedElements).forEach(([elementKey, elementValue]) => {
        if (Array.isArray(elementValue)) {
            updatedElements[elementKey] = elementValue.map((element) => {
                const elementNodeId = element.name;

                const elementPosition = nodePositions[elementNodeId];

                const updatedElement = {
                    ...element,
                    metadata: {
                        ...element?.metadata,
                        ui: {
                            ...element?.metadata?.ui,
                            nodePosition: elementPosition || element?.metadata?.ui?.nodePosition,
                        },
                    },
                };

                if (updatedElement.clusterElements) {
                    updatedElement.clusterElements = updateClusterElementsPositions({
                        clusterElements: updatedElement.clusterElements,
                        nodePositions,
                    });
                }

                return updatedElement;
            });
        } else if (elementValue && typeof elementValue === 'object') {
            const elementNodeId = elementValue.name;

            const elementPosition = nodePositions[elementNodeId];

            updatedElements[elementKey] = {
                ...elementValue,
                metadata: {
                    ...elementValue?.metadata,
                    ui: {
                        ...elementValue?.metadata?.ui,
                        nodePosition: elementPosition || elementValue?.metadata?.ui?.nodePosition,
                    },
                },
            } as ClusterElementItemType;

            const updatedElement = updatedElements[elementKey] as ClusterElementItemType;

            if (updatedElement.clusterElements) {
                updatedElement.clusterElements = updateClusterElementsPositions({
                    clusterElements: updatedElement.clusterElements,
                    nodePositions,
                });
            }
        }
    });

    return updatedElements;
}
