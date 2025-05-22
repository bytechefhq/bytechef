import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';

export function initializeClusterElementsObject(
    rootClusterElementDefinition: ComponentDefinition,
    clusterElementsData: ClusterElementsType
) {
    const clusterElements: ClusterElementsType = {};

    if (rootClusterElementDefinition?.clusterElementTypes) {
        rootClusterElementDefinition.clusterElementTypes.forEach((elementType) => {
            const objectKey = convertNameToCamelCase(elementType.name!);

            if (
                (clusterElementsData && clusterElementsData[objectKey] !== undefined) ||
                (clusterElementsData && clusterElementsData[objectKey] !== null)
            ) {
                if (elementType.multipleElements) {
                    if (Array.isArray(clusterElementsData[objectKey])) {
                        clusterElements[objectKey] = (clusterElementsData[objectKey] as ClusterElementItemType[]).map(
                            (element) => ({
                                label: element.label,
                                name: element.name,
                                parameters: element.parameters || {},
                                type: element.type,
                            })
                        );
                    } else {
                        clusterElements[objectKey] = [];
                    }
                } else {
                    const element = clusterElementsData[objectKey];

                    if (element && !Array.isArray(element)) {
                        clusterElements[objectKey] = {
                            label: element.label,
                            name: element.name,
                            parameters: element.parameters || {},
                            type: element.type,
                        };
                    } else {
                        clusterElements[objectKey] = null;
                    }
                }
            } else {
                clusterElements[objectKey] = elementType.multipleElements ? [] : null;
            }
        });
    }

    return clusterElements;
}

export function convertNameToCamelCase(typeName: string): string {
    let convertedTypeName = typeName.toLowerCase();

    if (typeName.includes('_')) {
        const [firstWord, ...remainingWords] = typeName.toLowerCase().split('_');
        convertedTypeName =
            firstWord + remainingWords.map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join('');
    }

    return convertedTypeName;
}

export function convertNameToSnakeCase(type: string): string {
    return type.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`).toUpperCase();
}
