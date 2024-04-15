import LoadingIcon from '@/components/LoadingIcon';
import {PropertiesDataSourceModel, PropertyModel} from '@/middleware/platform/configuration';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {useGetWorkflowNodeDynamicPropertiesQuery} from '@/queries/platform/workflowNodeDynamicProperties.queries';
import {ComponentType, CurrentComponentDefinitionType} from '@/types/types';
import {useEffect, useState} from 'react';

import Property from '../Property';

interface PropertyDynamicPropertiesProps {
    currentActionName?: string;
    currentComponentDefinition: CurrentComponentDefinitionType;
    currentComponent: ComponentType;
    currentNodeConnectionId?: number;
    loadDependsOnValues?: Array<string>;
    name?: string;
    propertiesDataSource?: PropertiesDataSourceModel;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    taskParameterValue?: any;
}

const PropertyDynamicProperties = ({
    currentActionName,
    currentComponent,
    currentComponentDefinition,
    currentNodeConnectionId,
    loadDependsOnValues,
    name,
    propertiesDataSource,
    taskParameterValue,
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
        !!propertiesDataSource &&
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
        <ul key={(loadDependsOnValues ?? []).join('')}>
            {subProperties.map((property, index) => {
                const propertyDefaultValue = property.name ? taskParameterValue?.[property.name] : '';

                return (
                    <Property
                        actionName={currentActionName}
                        currentComponent={currentComponent}
                        currentComponentDefinition={currentComponentDefinition}
                        key={`${property.name}_${index}_${(loadDependsOnValues ?? []).join('')}`}
                        objectName={name}
                        path={name}
                        property={property}
                        taskParameterValue={propertyDefaultValue}
                    />
                );
            })}
        </ul>
    ) : (
        <></>
    );
};

export default PropertyDynamicProperties;
