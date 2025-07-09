import {ROOT_CLUSTER_HANDLE_STEP, ROOT_CLUSTER_WIDTH} from '@/shared/constants';
import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';

export function initializeClusterElementsObject(
    clusterElementsData: ClusterElementsType,
    rootClusterElementDefinition: ComponentDefinition
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
                    clusterElements: element.clusterElements,
                    label: element.label,
                    metadata: element.metadata,
                    name: element.name,
                    parameters: element.parameters,
                    type: element.type,
                }));
            } else {
                const element = clusterElementsData[clusterElementType];

                if (element && !Array.isArray(element)) {
                    clusterElements[clusterElementType] = {
                        clusterElements: element.clusterElements,
                        label: element.label,
                        metadata: element.metadata,
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

export function calculateNodeWidth(handleCount: number): number {
    const baseWidth = ROOT_CLUSTER_WIDTH;
    const handleStep = ROOT_CLUSTER_HANDLE_STEP;

    if (!handleCount || handleCount <= 4) {
        return baseWidth;
    }

    return baseWidth + (handleCount - 4) * handleStep;
}

export function getHandlePosition(index: number, totalHandles: number, nodeWidth: number): number {
    const nodeEdgeBuffer = nodeWidth * 0.1;

    const usableNodeWidth = nodeWidth - nodeEdgeBuffer * 2;

    if (totalHandles === 1) {
        return nodeWidth / 2;
    }

    const stepWidth = usableNodeWidth / (totalHandles - 1);

    const handlePosition = nodeEdgeBuffer + stepWidth * index;

    return handlePosition;
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

export function getClusterElementsLabel(clusterElementType: string) {
    clusterElementType = clusterElementType.replace(/([a-z])([A-Z])/g, '$1 $2');

    return clusterElementType.charAt(0).toUpperCase() + clusterElementType.slice(1);
}
