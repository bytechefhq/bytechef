import {Skeleton} from '@/components/ui/skeleton';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {
    useGetClusterElementDynamicPropertiesQuery,
    useGetWorkflowNodeDynamicPropertiesQuery,
} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PropertyAllType} from '@/shared/types';
import {useEffect, useMemo, useState} from 'react';

import useWorkflowEditorStore from '../../../stores/useWorkflowEditorStore';
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
    const [subProperties, setSubProperties] = useState<PropertyAllType[]>([]);
    const [lastProcessedKey, setLastProcessedKey] = useState('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const {rootClusterElementNodeData} = useWorkflowEditorStore();

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

    const clusterElementQueryOptions = useMemo(
        () => ({
            lookupDependsOnValuesKey,
            request: {
                clusterElementType: currentNode?.clusterElementType ?? '',
                clusterElementWorkflowNodeName: currentNode?.workflowNodeName ?? '',
                environmentId: currentEnvironmentId,
                id: workflow.id!,
                propertyName: name!,
                workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
            },
        }),
        [
            lookupDependsOnValuesKey,
            currentNode?.clusterElementType,
            currentNode?.workflowNodeName,
            currentEnvironmentId,
            workflow.id,
            name,
            rootClusterElementNodeData?.workflowNodeName,
        ]
    );

    const queryEnabled = useMemo(
        () =>
            (lookupDependsOnPaths?.length
                ? lookupDependsOnValues?.every((loadDependencyValue) => !!loadDependencyValue)
                : true) && enabled,
        [lookupDependsOnPaths?.length, lookupDependsOnValues, enabled]
    );

    const {data: properties, isLoading} = useGetWorkflowNodeDynamicPropertiesQuery(
        queryOptions,
        Boolean(queryEnabled && !currentNode?.clusterElementType)
    );

    const {data: clusterElementProperties, isLoading: isClusterElementPropertiesLoading} =
        useGetClusterElementDynamicPropertiesQuery(
            clusterElementQueryOptions,
            Boolean(queryEnabled && currentNode?.clusterElementType)
        );

    // Update subProperties and track which key generated these properties
    useEffect(() => {
        if (properties) {
            setSubProperties(properties);

            setLastProcessedKey(lookupDependsOnValuesKey);
        } else if (clusterElementProperties) {
            setSubProperties(clusterElementProperties);

            setLastProcessedKey(lookupDependsOnValuesKey);
        } else {
            setSubProperties([]);
        }
    }, [properties, lookupDependsOnValuesKey, clusterElementProperties]);

    const isPending = lookupDependsOnValuesKey !== lastProcessedKey;

    // Guard against primitive parameterValue (e.g., string) so bracket access doesn't hit prototype methods
    const paramValueObject = useMemo(
        () =>
            parameterValue && typeof parameterValue === 'object' && !Array.isArray(parameterValue)
                ? parameterValue
                : undefined,
        [parameterValue]
    );

    if ((isLoading || isClusterElementPropertiesLoading || isPending) && queryEnabled) {
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
            {subProperties.map((property, index) => {
                let defaultValue = property.defaultValue;

                if (defaultValue === undefined) {
                    defaultValue = property.name ? (paramValueObject?.[property.name] ?? '') : '';
                }

                return (
                    <Property
                        dynamicPropertySource={name}
                        key={`${property.name}_${index}_${lastProcessedKey}_property`}
                        objectName={name}
                        operationName={currentOperationName}
                        parameterValue={defaultValue}
                        path={path}
                        property={{
                            ...property,
                            defaultValue,
                        }}
                    />
                );
            })}
        </ul>
    );
};

export default PropertyDynamicProperties;
