import LoadingIcon from '@/components/LoadingIcon';
import {PropertyModel} from '@/middleware/platform/configuration';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useGetWorkflowNodeDynamicPropertiesQuery} from '@/queries/platform/workflowNodeDynamicProperties.queries';
import {ComponentType, CurrentComponentDefinitionType} from '@/types/types';
import {useEffect, useState} from 'react';

import Property from '../Property';

interface PropertyDynamicPropertiesProps {
    currentOperationName?: string;
    currentComponentDefinition: CurrentComponentDefinitionType;
    currentComponent: ComponentType;
    currentNodeConnectionId?: number;
    loadDependsOnValues?: Array<string>;
    name?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameterValue?: any;
}

const PropertyDynamicProperties = ({
    currentComponent,
    currentComponentDefinition,
    currentNodeConnectionId,
    currentOperationName,
    loadDependsOnValues,
    name,
    parameterValue,
}: PropertyDynamicPropertiesProps) => {
    const [subProperties, setSubProperties] = useState<PropertyModel[]>();

    const {workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {data: properties, isLoading} = useGetWorkflowNodeDynamicPropertiesQuery(
        {
            loadDependencyValueKey: (loadDependsOnValues ?? []).join(','),
            request: {
                id: workflow.id!,
                propertyName: name!,
                workflowNodeName: currentNode.name!,
            },
        },
        !!(loadDependsOnValues ?? []).length &&
            (loadDependsOnValues ? loadDependsOnValues.every((loadDependencyValue) => !!loadDependencyValue) : false) &&
            !!currentNodeConnectionId
    );

    useEffect(() => {
        setSubProperties(properties);
    }, [properties]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center">
                <LoadingIcon /> Loading properties...
            </div>
        );
    }

    return subProperties ? (
        <ul className="space-y-4" key={(loadDependsOnValues ?? []).join('')}>
            {subProperties.map((property, index) => {
                const propertyDefaultValue = property.name ? parameterValue?.[property.name] : '';

                return (
                    <Property
                        currentComponent={currentComponent}
                        currentComponentDefinition={currentComponentDefinition}
                        key={`${property.name}_${index}_${(loadDependsOnValues ?? []).join('')}`}
                        objectName={name}
                        operationName={currentOperationName}
                        parameterValue={propertyDefaultValue}
                        path={name}
                        property={property}
                    />
                );
            })}
        </ul>
    ) : (
        <></>
    );
};

export default PropertyDynamicProperties;
