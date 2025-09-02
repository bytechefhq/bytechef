import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';

import {isPlainObject} from '../../cluster-element-editor/utils/clusterElementsUtils';

export const removeClusterElementPosition = (
    clusterElements: ClusterElementsType,
    clickedNodeName: string
): ClusterElementsType => {
    const updatedClusterElements: ClusterElementsType = {};

    Object.entries(clusterElements).forEach(([elementKey, elementValue]) => {
        if (Array.isArray(elementValue)) {
            updatedClusterElements[elementKey] = elementValue.map((element) => {
                const positionRemovalRequired = element.name === clickedNodeName;

                const updatedElement: ClusterElementItemType = {
                    ...element,
                    metadata: {
                        ...element?.metadata,
                        ui: {
                            ...element?.metadata?.ui,
                            nodePosition: positionRemovalRequired ? undefined : element?.metadata?.ui?.nodePosition,
                        },
                    },
                };

                if (updatedElement.clusterElements) {
                    updatedElement.clusterElements = removeClusterElementPosition(
                        updatedElement.clusterElements,
                        clickedNodeName
                    );
                }

                return updatedElement;
            });
        } else if (elementValue && isPlainObject(elementValue)) {
            const positionRemovalRequired = elementValue.name === clickedNodeName;

            const updatedElement = {
                ...elementValue,
                metadata: {
                    ...elementValue?.metadata,
                    ui: {
                        ...elementValue?.metadata?.ui,
                        nodePosition: positionRemovalRequired ? undefined : elementValue?.metadata?.ui?.nodePosition,
                    },
                },
            };

            if (updatedElement.clusterElements) {
                updatedElement.clusterElements = removeClusterElementPosition(
                    updatedElement.clusterElements,
                    clickedNodeName
                );
            }

            updatedClusterElements[elementKey] = updatedElement;
        }
    });

    return updatedClusterElements;
};
