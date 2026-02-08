import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementItemType, ClusterElementsType, NodeDataType, PropertyAllType} from '@/shared/types';

import {isPlainObject} from '../../cluster-element-editor/utils/clusterElementsUtils';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';

type FieldUpdateType = {
    field: 'operation' | 'label' | 'description';
    value: string;
};

interface CreateUpdatedElementProps {
    currentComponentDefinition: ComponentDefinition;
    currentOperationProperties?: Array<PropertyAllType>;
    element: ClusterElementItemType | NodeDataType;
    fieldUpdate: FieldUpdateType;
}

export function createUpdatedElement({
    currentComponentDefinition,
    currentOperationProperties,
    element,
    fieldUpdate,
}: CreateUpdatedElementProps) {
    switch (fieldUpdate.field) {
        case 'operation':
            return {
                ...element,
                clusterElementName: fieldUpdate.value,
                metadata: {
                    ui: {
                        nodePosition: element.metadata?.ui?.nodePosition
                            ? element.metadata?.ui?.nodePosition
                            : undefined,
                    },
                },
                operationName: fieldUpdate.value,
                parameters: getParametersWithDefaultValues({
                    properties: currentOperationProperties as Array<PropertyAllType>,
                }),
                type: `${currentComponentDefinition.name}/v${currentComponentDefinition.version}/${fieldUpdate.value}`,
            };
        case 'label':
            return {
                ...element,
                label: fieldUpdate.value,
            };
        case 'description':
            return {
                ...element,
                description: fieldUpdate.value,
            };
        default:
            return element;
    }
}

interface UpdateClusterRootElementFieldProps {
    currentComponentDefinition: ComponentDefinition;
    currentOperationProperties?: Array<PropertyAllType>;
    fieldUpdate: FieldUpdateType;
    mainRootElement: NodeDataType;
}

export function updateClusterRootElementField({
    currentComponentDefinition,
    currentOperationProperties,
    fieldUpdate,
    mainRootElement,
}: UpdateClusterRootElementFieldProps): NodeDataType {
    const updatedElementData = createUpdatedElement({
        currentComponentDefinition,
        currentOperationProperties,
        element: mainRootElement,
        fieldUpdate,
    });

    return {
        ...mainRootElement,
        ...updatedElementData,
        componentName: mainRootElement.componentName,
        name: mainRootElement.name,
        workflowNodeName: mainRootElement.workflowNodeName,
    } as NodeDataType;
}

interface UpdateNestedClusterElementFieldProps {
    clusterElements: ClusterElementsType;
    currentComponentDefinition: ComponentDefinition;
    currentOperationProperties?: Array<PropertyAllType>;
    elementName: string;
    fieldUpdate: FieldUpdateType;
}

export function updateNestedClusterElementField({
    clusterElements,
    currentComponentDefinition,
    currentOperationProperties,
    elementName,
    fieldUpdate,
}: UpdateNestedClusterElementFieldProps): ClusterElementsType {
    if (!clusterElements || Object.keys(clusterElements).length === 0) {
        return clusterElements;
    }

    const updatedClusterElements: ClusterElementsType = {...clusterElements};

    const processElement = (element: ClusterElementItemType) => {
        if (element.name === elementName) {
            return createUpdatedElement({
                currentComponentDefinition,
                currentOperationProperties,
                element,
                fieldUpdate,
            }) as ClusterElementItemType;
        }

        // Process nested elements
        if (element.clusterElements) {
            return {
                ...element,
                clusterElements: updateNestedClusterElementField({
                    clusterElements: element.clusterElements,
                    currentComponentDefinition,
                    currentOperationProperties,
                    elementName,
                    fieldUpdate,
                }),
            };
        }

        return element;
    };

    for (const key in updatedClusterElements) {
        if (Object.prototype.hasOwnProperty.call(updatedClusterElements, key)) {
            const nestedContent = updatedClusterElements[key];

            if (Array.isArray(nestedContent)) {
                updatedClusterElements[key] = nestedContent.map(processElement);
            } else if (isPlainObject(nestedContent) && nestedContent !== null) {
                updatedClusterElements[key] = processElement(nestedContent as ClusterElementItemType);
            }
        }
    }

    return updatedClusterElements;
}
