import {PropertyAllType} from '@/shared/types';
import {useEffect, useMemo, useState} from 'react';

import isOperationDefinitionForNode from './isOperationDefinitionForNode';

const EMPTY_OPERATION_PROPERTIES: Array<PropertyAllType> = [];

interface OperationPropertiesI {
    nodeName?: string;
    properties: Array<PropertyAllType>;
}

interface UseNodeOperationPropertiesProps {
    currentNode?: {
        clusterElementType?: string;
        componentName?: string;
        name?: string;
        taskDispatcher?: boolean;
        type?: string;
    };
    operationDefinition?: {
        componentName?: string;
        name?: string;
        properties?: Array<PropertyAllType>;
    };
}

export default function useNodeOperationProperties({
    currentNode,
    operationDefinition,
}: UseNodeOperationPropertiesProps): Array<PropertyAllType> {
    const [operationProperties, setOperationProperties] = useState<OperationPropertiesI>({
        properties: EMPTY_OPERATION_PROPERTIES,
    });

    const nodeOperationProperties = isOperationDefinitionForNode(operationDefinition, currentNode)
        ? operationDefinition?.properties
        : undefined;

    const currentNodeName = currentNode?.name;

    const currentOperationProperties = useMemo(
        () =>
            operationProperties.nodeName === currentNodeName
                ? operationProperties.properties
                : EMPTY_OPERATION_PROPERTIES,
        [currentNodeName, operationProperties]
    );

    useEffect(() => {
        if (currentNodeName && nodeOperationProperties) {
            setOperationProperties({nodeName: currentNodeName, properties: nodeOperationProperties});
        }
    }, [currentNodeName, nodeOperationProperties]);

    return currentOperationProperties;
}
