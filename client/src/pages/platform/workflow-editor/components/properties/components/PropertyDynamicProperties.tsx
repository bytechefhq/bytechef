import {Skeleton} from '@/components/ui/skeleton';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Property as PropertyModel} from '@/shared/middleware/platform/configuration';
import {useGetWorkflowNodeDynamicPropertiesQuery} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {useEffect, useMemo, useState} from 'react';

import Property from '../Property';

interface PropertyDynamicPropertiesProps {
    currentOperationName?: string;
    enabled: boolean;
    lookupDependsOnPaths?: Array<string>;
    lookupDependsOnValues?: Array<string>;
    name?: string;
    path?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameterValue?: any;
}

const PropertyDynamicProperties = ({
    currentOperationName,
    enabled,
    lookupDependsOnPaths,
    lookupDependsOnValues,
    name,
    parameterValue,
    path,
}: PropertyDynamicPropertiesProps) => {
    const [subProperties, setSubProperties] = useState<PropertyModel[]>();

    const {workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const queryOptions = useMemo(
        () => ({
            lookupDependsOnValuesKey: (lookupDependsOnValues ?? []).join(','),
            request: {
                id: workflow.id!,
                propertyName: name!,
                workflowNodeName: currentNode?.name ?? '',
            },
        }),
        [lookupDependsOnValues, name, workflow.id, currentNode]
    );

    const queryEnabled = useMemo(
        () =>
            (lookupDependsOnPaths?.length
                ? lookupDependsOnValues?.every((loadDependencyValue) => !!loadDependencyValue)
                : true) && enabled,
        [lookupDependsOnPaths?.length, lookupDependsOnValues, enabled]
    );

    const {data: properties, isLoading} = useGetWorkflowNodeDynamicPropertiesQuery(queryOptions, Boolean(queryEnabled));

    useEffect(() => {
        setSubProperties(properties);
    }, [properties]);

    if (isLoading) {
        return (
            <ul className="flex flex-col gap-4">
                <li className="flex flex-col space-y-1">
                    <Skeleton className="h-5 w-1/4" />

                    <Skeleton className="h-9 w-full" />
                </li>

                <li className="flex flex-col space-y-1">
                    <Skeleton className="h-5 w-1/4" />

                    <Skeleton className="h-9 w-full" />
                </li>

                <li className="flex flex-col space-y-1">
                    <Skeleton className="h-5 w-1/4" />

                    <Skeleton className="h-9 w-full" />
                </li>
            </ul>
        );
    }

    if (!subProperties) {
        return <></>;
    }

    return (
        <ul className="space-y-4" key={`${(lookupDependsOnValues ?? []).join('')}_DynamicProperty`}>
            {subProperties.map((property, index) => (
                <Property
                    key={`${property.name}_${index}_${(lookupDependsOnValues ?? []).join('')}_property`}
                    objectName={name}
                    operationName={currentOperationName}
                    parameterValue={property.name ? parameterValue?.[property.name] : ''}
                    path={path}
                    property={{
                        ...property,
                        defaultValue: property.name ? parameterValue?.[property.name] : '',
                    }}
                />
            ))}
        </ul>
    );
};

export default PropertyDynamicProperties;
