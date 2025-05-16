import {Skeleton} from '@/components/ui/skeleton';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Property as PropertyModel} from '@/shared/middleware/platform/configuration';
import {useGetWorkflowNodeDynamicPropertiesQuery} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {Fragment, useEffect, useMemo, useState} from 'react';

import getFormattedDependencyKey from '../../../utils/getFormattedDependencyKey';
import Property from '../Property';

interface PropertyDynamicPropertiesProps {
    currentOperationName?: string;
    enabled: boolean;
    lookupDependsOnPaths?: Array<unknown>;
    lookupDependsOnValues?: Array<unknown>;
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
    const [lastProcessedKey, setLastProcessedKey] = useState('');

    const {workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const lookupDependsOnValuesKey = getFormattedDependencyKey(lookupDependsOnValues);

    const queryOptions = useMemo(
        () => ({
            lookupDependsOnValuesKey,
            request: {
                id: workflow.id!,
                propertyName: name!,
                workflowNodeName: currentNode?.name ?? '',
            },
        }),
        [lookupDependsOnValuesKey, workflow.id, name, currentNode?.name]
    );

    const queryEnabled = useMemo(
        () =>
            (lookupDependsOnPaths?.length
                ? lookupDependsOnValues?.every((loadDependencyValue) => !!loadDependencyValue)
                : true) && enabled,
        [lookupDependsOnPaths?.length, lookupDependsOnValues, enabled]
    );

    const {data: properties, isLoading} = useGetWorkflowNodeDynamicPropertiesQuery(queryOptions, Boolean(queryEnabled));

    // Update subProperties and track which key generated these properties
    useEffect(() => {
        if (properties) {
            setSubProperties(properties);

            setLastProcessedKey(lookupDependsOnValuesKey);
        }
    }, [properties, lookupDependsOnValuesKey]);

    const isPending = lookupDependsOnValuesKey !== lastProcessedKey;

    if ((isLoading || isPending) && queryEnabled) {
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
        <ul className="space-y-4">
            {subProperties.map((property, index) => (
                <Property
                    key={`${property.name}_${index}_${lastProcessedKey}_property`}
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
