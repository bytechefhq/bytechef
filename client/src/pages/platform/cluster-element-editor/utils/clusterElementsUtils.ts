import {ROOT_CLUSTER_HANDLE_STEP, ROOT_CLUSTER_WIDTH} from '@/shared/constants';
import {ComponentDefinition, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {ClusterElementItemType, ClusterElementsType, WorkflowNodeType} from '@/shared/types';

interface InitializeClusterElementsObjectProps {
    clusterElementsData: ClusterElementsType;
    mainClusterRootTask: WorkflowTask;
    rootClusterElementDefinition: ComponentDefinition;
}

export function initializeClusterElementsObject({
    clusterElementsData,
    mainClusterRootTask,
    rootClusterElementDefinition,
}: InitializeClusterElementsObjectProps) {
    const clusterElements: ClusterElementsType = {};

    if (!rootClusterElementDefinition.clusterElementTypes) {
        return clusterElements;
    }

    const mainClusterRootType = mainClusterRootTask.type?.split('/')[2];

    const filteredClusterElementTypes = (() => {
        if (
            rootClusterElementDefinition.actionClusterElementTypes &&
            Object.keys(rootClusterElementDefinition.actionClusterElementTypes).length > 0 &&
            mainClusterRootType
        ) {
            return rootClusterElementDefinition.actionClusterElementTypes[mainClusterRootType] || [];
        } else {
            return rootClusterElementDefinition.clusterElementTypes.map((type) => type.name);
        }
    })();

    rootClusterElementDefinition.clusterElementTypes.forEach((elementType) => {
        const matchingType = filteredClusterElementTypes.some((type) => {
            return type === elementType.name;
        });

        if (!matchingType) {
            return;
        }

        const clusterElementType = convertNameToCamelCase(elementType.name || '');

        const elementData = clusterElementsData?.[clusterElementType];

        if (elementType.multipleElements) {
            if (Array.isArray(elementData) && elementData.length > 0) {
                clusterElements[clusterElementType] = elementData.map((element) => ({
                    clusterElements: element.clusterElements,
                    label: element.label,
                    metadata: element.metadata,
                    name: element.name,
                    parameters: element.parameters || {},
                    type: element.type,
                }));
            } else {
                clusterElements[clusterElementType] = [];
            }
        } else {
            if (elementData && isPlainObject(elementData)) {
                clusterElements[clusterElementType] = {
                    clusterElements: elementData.clusterElements,
                    label: elementData.label,
                    metadata: elementData.metadata,
                    name: elementData.name,
                    parameters: elementData.parameters || {},
                    type: elementData.type,
                };
            } else {
                clusterElements[clusterElementType] = null;
            }
        }
    });

    return clusterElements;
}

interface AddElementToClusterRootProps {
    clusterElementTypeLabel: string;
    clusterElementValue: ClusterElementItemType;
    clusterElements: ClusterElementsType;
    isMultipleElements: boolean;
}

export function addElementToClusterRoot({
    clusterElementTypeLabel,
    clusterElementValue,
    clusterElements,
    isMultipleElements,
}: AddElementToClusterRootProps) {
    if (isMultipleElements) {
        return {
            ...clusterElements,
            [clusterElementTypeLabel]: [
                ...(Array.isArray(clusterElements[clusterElementTypeLabel])
                    ? clusterElements[clusterElementTypeLabel]
                    : []),
                clusterElementValue,
            ],
        };
    } else {
        return {
            ...clusterElements,
            [clusterElementTypeLabel]: clusterElementValue,
        };
    }
}

function getTypeSegments(type: string = ''): {componentName: string; version: number; operationName: string} {
    const segments = type.split('/');

    return {
        componentName: segments[0] || '',
        operationName: segments[2] || '',
        version: +(segments[1] || '').replace('v', '') || 1,
    };
}

export function extractClusterElementComponentOperations(
    clusterElements: ClusterElementsType,
    existingClusterElementsOperations: WorkflowNodeType[] = []
): WorkflowNodeType[] {
    if (!clusterElements) {
        return existingClusterElementsOperations;
    }

    const processClusterElement = (
        collectedOperations: WorkflowNodeType[],
        element: ClusterElementItemType
    ): WorkflowNodeType[] => {
        if (!element) {
            return collectedOperations;
        }

        const {componentName, operationName, version} = getTypeSegments(element.type);

        // Add the current element's operation to the collectedOperations
        collectedOperations.push({
            name: componentName,
            operationName,
            version,
            workflowNodeName: element.name || '',
        });

        // Process nested cluster elements
        if (element.clusterElements) {
            return extractClusterElementComponentOperations(element.clusterElements, collectedOperations);
        }

        return collectedOperations;
    };

    // Process all elements in the clusterElements object
    return Object.values(clusterElements).reduce((collectedOperations, element) => {
        if (Array.isArray(element)) {
            return element.reduce(processClusterElement, collectedOperations);
        } else if (element && isPlainObject(element)) {
            return processClusterElement(collectedOperations, element);
        }

        return collectedOperations;
    }, existingClusterElementsOperations);
}

export function getClusterElementByName(clusterElements: ClusterElementsType, elementName: string) {
    if (!clusterElements) {
        console.error('Cluster elements not found');

        return undefined;
    }

    const elements = Object.values(clusterElements);
    let matchingElement: ClusterElementItemType | undefined;

    elements.forEach((element) => {
        if (matchingElement) {
            return;
        }

        if (Array.isArray(element)) {
            element.forEach((element) => {
                if (matchingElement) {
                    return;
                }

                if (element.name === elementName) {
                    matchingElement = element;

                    return;
                }

                if (element.clusterElements) {
                    const matchingNestedElement = getClusterElementByName(element.clusterElements, elementName);

                    if (matchingNestedElement) {
                        matchingElement = matchingNestedElement;
                    }
                }
            });
        } else if (isPlainObject(element)) {
            if (matchingElement) {
                return;
            }

            if (element.name === elementName) {
                matchingElement = element as ClusterElementItemType;

                return;
            }

            if (element.clusterElements) {
                const matchingNestedElement = getClusterElementByName(element.clusterElements, elementName);

                if (matchingNestedElement) {
                    matchingElement = matchingNestedElement;
                }
            }
        }
    });

    return matchingElement;
}

interface GetClusterElementTypesCountProps {
    clusterRootComponentDefinition: ComponentDefinition;
    operationName?: string;
}

export function getClusterElementTypesCount({
    clusterRootComponentDefinition,
    operationName,
}: GetClusterElementTypesCountProps): number {
    if (!clusterRootComponentDefinition || !clusterRootComponentDefinition.clusterElementTypes) {
        return 0;
    }

    if (!operationName) {
        return clusterRootComponentDefinition.clusterElementTypes.length;
    }

    const actionTypes = clusterRootComponentDefinition.actionClusterElementTypes;

    if (!actionTypes || Object.keys(actionTypes).length === 0) {
        return clusterRootComponentDefinition.clusterElementTypes.length;
    }

    const operationElementTypes = actionTypes[operationName];

    if (!operationElementTypes || operationElementTypes.length === 0) {
        return clusterRootComponentDefinition.clusterElementTypes.length;
    }

    const filteredElementTypes = clusterRootComponentDefinition.clusterElementTypes.filter((elementType) =>
        operationElementTypes.includes(elementType.name || '')
    );

    return filteredElementTypes.length;
}

export function calculateNodeWidth(handleCount: number): number {
    const baseWidth = ROOT_CLUSTER_WIDTH;
    const handleStep = ROOT_CLUSTER_HANDLE_STEP;

    if (!handleCount || handleCount <= 4) {
        return baseWidth;
    }

    return baseWidth + (handleCount - 4) * handleStep;
}

interface GetHandlePositionProps {
    handlesCount: number;
    index: number;
    nodeWidth: number;
}

export function getHandlePosition({handlesCount, index, nodeWidth}: GetHandlePositionProps): number {
    const nodeEdgeBuffer = nodeWidth * 0.1;

    const usableNodeWidth = nodeWidth - nodeEdgeBuffer * 2;

    if (handlesCount === 1) {
        return nodeWidth / 2;
    }

    const stepWidth = usableNodeWidth / (handlesCount - 1);

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

export function isPlainObject(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}
