import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';

export function initializeClusterElementsObject(
    rootClusterElementDefinition: ComponentDefinition,
    clusterElementsData: ClusterElementsType
) {
    const clusterElements: ClusterElementsType = {};

    if (!rootClusterElementDefinition.clusterElementTypes) {
        return clusterElements;
    }

    rootClusterElementDefinition.clusterElementTypes.forEach((elementType) => {
        const clusterElementType = convertNameToCamelCase(elementType.name || '');

        const hasElementData = clusterElementsData?.[clusterElementType] != null;

        if (hasElementData) {
            if (elementType.multipleElements) {
                if (!Array.isArray(clusterElementsData[clusterElementType])) {
                    clusterElements[clusterElementType] = [];
                }

                clusterElements[clusterElementType] = (
                    clusterElementsData[clusterElementType] as ClusterElementItemType[]
                ).map((element) => ({
                    label: element.label,
                    name: element.name,
                    parameters: element.parameters || {},
                    type: element.type,
                }));
            } else {
                const element = clusterElementsData[clusterElementType];

                if (element && !Array.isArray(element)) {
                    clusterElements[clusterElementType] = {
                        label: element.label,
                        name: element.name,
                        parameters: element.parameters || {},
                        type: element.type,
                    };
                }
            }
        } else {
            clusterElements[clusterElementType] = elementType.multipleElements ? [] : null;
        }
    });

    return clusterElements;
}

export function convertNameToCamelCase(typeName: string): string {
    let convertedTypeName = typeName.toLowerCase();

    if (typeName.includes('_')) {
        const [firstWord, ...remainingWords] = typeName.toLowerCase().split('_');

        const capitalizedWords = remainingWords.map((word) => word.charAt(0).toUpperCase() + word.slice(1));

        convertedTypeName = `${firstWord}${capitalizedWords.join('')}`;
    }

    return convertedTypeName;
}

export function convertNameToSnakeCase(type: string): string {
    return type.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`).toUpperCase();
}
