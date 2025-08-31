import {Skeleton} from '@/components/ui/skeleton';
import {useEnvironmentStore} from '@/pages/automation/stores/useEnvironmentStore';
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
    const [subProperties, setSubProperties] = useState<PropertyModel[]>([]);
    const [lastProcessedKey, setLastProcessedKey] = useState('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);

    const lookupDependsOnValuesKey = getFormattedDependencyKey(lookupDependsOnValues);

    const queryOptions = useMemo(
        () => ({
            lookupDependsOnValuesKey,
            request: {
                environmentId: currentEnvironmentId,
                id: workflow.id!,
                propertyName: name!,
                workflowNodeName: currentNode?.name ?? '',
            },
        }),
        [currentEnvironmentId, lookupDependsOnValuesKey, workflow.id, name, currentNode?.name]
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
        } else {
            setSubProperties([]);
        }
    }, [properties, lookupDependsOnValuesKey]);

    const isPending = lookupDependsOnValuesKey !== lastProcessedKey;

    if ((isLoading || isPending) && queryEnabled) {
        return (
            <ul className="flex flex-col gap-4">
                {Array.from({length: 3}).map((_, index) => (
                    <li className="flex flex-col space-y-1" key={index}>
                        <Skeleton className="h-5 w-1/4" />

                        <Skeleton className="h-9 w-full" />
                    </li>
                ))}
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
