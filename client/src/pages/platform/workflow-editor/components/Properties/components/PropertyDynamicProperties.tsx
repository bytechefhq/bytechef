import LoadingIcon from '@/components/LoadingIcon';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyModel} from '@/shared/middleware/platform/configuration';
import {useGetWorkflowNodeDynamicPropertiesQuery} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {useEffect, useState} from 'react';

import Property from '../Property';

interface PropertyDynamicPropertiesProps {
    currentOperationName?: string;
    currentNodeConnectionId?: number;
    lookupDependsOnValues?: Array<string>;
    name?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameterValue?: any;
}

const PropertyDynamicProperties = ({
    currentNodeConnectionId,
    currentOperationName,
    lookupDependsOnValues,
    name,
    parameterValue,
}: PropertyDynamicPropertiesProps) => {
    const [subProperties, setSubProperties] = useState<PropertyModel[]>();

    const {workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {data: properties, isLoading} = useGetWorkflowNodeDynamicPropertiesQuery(
        {
            lookupDependsOnValuesKey: (lookupDependsOnValues ?? []).join(','),
            request: {
                id: workflow.id!,
                propertyName: name!,
                workflowNodeName: currentNode?.name ?? '',
            },
        },
        !!(lookupDependsOnValues ?? []).length &&
            (lookupDependsOnValues
                ? lookupDependsOnValues.every((loadDependencyValue) => !!loadDependencyValue)
                : false) &&
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
        <ul className="space-y-4" key={(lookupDependsOnValues ?? []).join('')}>
            {subProperties.map((property, index) => {
                const propertyDefaultValue = property.name ? parameterValue?.[property.name] : '';

                return (
                    <Property
                        key={`${property.name}_${index}_${(lookupDependsOnValues ?? []).join('')}`}
                        objectName={name}
                        operationName={currentOperationName}
                        parameterValue={propertyDefaultValue}
                        path={`parameters.${name}`}
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
