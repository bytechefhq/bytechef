import {ClusterElementsType} from '@/shared/types';

import {isPlainObject} from '../../cluster-element-editor/utils/clusterElementsUtils';

interface FindAndRemoveClusterElementProps {
    clickedElementName: string;
    clickedElementType?: string;
    clusterElements: ClusterElementsType;
}

export default function findAndRemoveClusterElement({
    clickedElementName,
    clickedElementType,
    clusterElements,
}: FindAndRemoveClusterElementProps) {
    const clusterElementRemovalResult = {elementFound: false, elements: {...clusterElements}};

    Object.entries(clusterElementRemovalResult.elements).forEach(([elementType, elementValue]) => {
        if (clusterElementRemovalResult.elementFound) {
            return;
        }

        if (Array.isArray(elementValue)) {
            const elementIndex = elementValue.findIndex(
                (element) =>
                    element.name === clickedElementName && (!clickedElementType || elementType === clickedElementType)
            );

            // First level multiple elements deletion
            if (elementIndex >= 0) {
                clusterElementRemovalResult.elements[elementType] = elementValue.filter(
                    (element) => element.name !== clickedElementName
                );

                clusterElementRemovalResult.elementFound = true;

                return;
            }

            // Process deletion of elements nested in a multiple elements root
            clusterElementRemovalResult.elements[elementType] = elementValue.map((element) => {
                if (!element.clusterElements) {
                    return element;
                }

                const nestedResult = findAndRemoveClusterElement({
                    clickedElementName,
                    clickedElementType,
                    clusterElements: element.clusterElements,
                });

                if (nestedResult.elementFound) {
                    clusterElementRemovalResult.elementFound = true;

                    return {
                        ...element,
                        clusterElements: nestedResult.elements,
                    };
                }

                return element;
            });
        } else if (isPlainObject(elementValue)) {
            // First level single element deletion
            if (
                elementValue.name === clickedElementName &&
                (!clickedElementType || elementType === clickedElementType)
            ) {
                clusterElementRemovalResult.elements[elementType] = null;

                clusterElementRemovalResult.elementFound = true;

                return;
            }

            // Process deletion of nested single elements
            if (!clusterElementRemovalResult.elementFound && elementValue.clusterElements) {
                const nestedResult = findAndRemoveClusterElement({
                    clickedElementName,
                    clickedElementType,
                    clusterElements: elementValue.clusterElements,
                });

                if (nestedResult.elementFound) {
                    clusterElementRemovalResult.elementFound = true;

                    clusterElementRemovalResult.elements[elementType] = {
                        ...elementValue,
                        clusterElements: nestedResult.elements,
                    };
                }
            }
        }
    });

    return clusterElementRemovalResult;
}
