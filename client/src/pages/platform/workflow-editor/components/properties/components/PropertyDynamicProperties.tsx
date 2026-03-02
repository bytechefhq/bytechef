import {Skeleton} from '@/components/ui/skeleton';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useClusterElementPropertyDynamicPropertiesQuery} from '@/shared/middleware/graphql';
import {
    useGetClusterElementDynamicPropertiesQuery,
    useGetWorkflowNodeDynamicPropertiesQuery,
} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PropertyAllType} from '@/shared/types';
import {useEffect, useMemo, useState} from 'react';
import {Control, FieldValues, FormState} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowEditorStore from '../../../stores/useWorkflowEditorStore';
import getFormattedDependencyKey from '../../../utils/getFormattedDependencyKey';
import {useClusterElementContext} from '../ClusterElementContext';
import Property from '../Property';

interface PropertyDynamicPropertiesProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control?: Control<any, any>;
    controlPath?: string;
    currentOperationName?: string;
    enabled: boolean;
    formState?: FormState<FieldValues>;
    lookupDependsOnPaths?: Array<unknown>;
    lookupDependsOnValues?: Array<unknown>;
    name?: string;
    path?: string;

    parameterValue?: any;
    toolsMode?: boolean;
}

const PropertyDynamicProperties = ({
    control,
    controlPath,
    currentOperationName,
    enabled,
    formState,
    lookupDependsOnPaths,
    lookupDependsOnValues,
    name,
    parameterValue,
    path,
    toolsMode,
}: PropertyDynamicPropertiesProps) => {
    const [subProperties, setSubProperties] = useState<PropertyAllType[]>([]);
    const [lastProcessedKey, setLastProcessedKey] = useState('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {currentNode, operationChangeInProgress} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentNode: state.currentNode,
            operationChangeInProgress: state.operationChangeInProgress,
        }))
    );

    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const clusterElementContext = useClusterElementContext();

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
                lookupDependsOnPaths: lookupDependsOnPaths as string[] | undefined,
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
            lookupDependsOnPaths,
            name,
            rootClusterElementNodeData?.workflowNodeName,
        ]
    );

    const queryEnabled = useMemo(
        () =>
            (lookupDependsOnPaths?.length
                ? lookupDependsOnValues?.every((loadDependencyValue) => {
                      if (loadDependencyValue == null) {
                          return false;
                      }

                      if (control && typeof loadDependencyValue === 'string') {
                          return (
                              loadDependencyValue !== '' &&
                              !loadDependencyValue.startsWith('=') &&
                              !loadDependencyValue.includes('${')
                          );
                      }

                      return true;
                  })
                : true) &&
            enabled &&
            !operationChangeInProgress,
        [control, lookupDependsOnPaths?.length, lookupDependsOnValues, enabled, operationChangeInProgress]
    );

    const {data: properties, isLoading} = useGetWorkflowNodeDynamicPropertiesQuery(
        queryOptions,
        Boolean(queryEnabled && !currentNode?.clusterElementType && !clusterElementContext)
    );

    const {data: clusterElementProperties, isLoading: isClusterElementPropertiesLoading} =
        useGetClusterElementDynamicPropertiesQuery(
            clusterElementQueryOptions,
            Boolean(queryEnabled && currentNode?.clusterElementType && !clusterElementContext)
        );

    // Cluster element context fallback: resolve lookup dependencies from form values
    const clusterElementInputParameters = useMemo(() => {
        if (!clusterElementContext?.inputParameters) {
            return undefined;
        }

        const filtered: Record<string, unknown> = {};

        for (const [parameterKey, parameterValue] of Object.entries(clusterElementContext.inputParameters)) {
            if (
                typeof parameterValue === 'string' &&
                (parameterValue.startsWith('=') || parameterValue.includes('${'))
            ) {
                continue;
            }

            filtered[parameterKey] = parameterValue;
        }

        return filtered;
    }, [clusterElementContext?.inputParameters]);

    const clusterElementLookupDependsOnValues = useMemo(() => {
        if (!clusterElementInputParameters || !lookupDependsOnPaths?.length) {
            return undefined;
        }

        return (lookupDependsOnPaths as string[]).map(
            (dependencyPath) => clusterElementInputParameters[dependencyPath]
        );
    }, [clusterElementInputParameters, lookupDependsOnPaths]);

    const clusterElementContextQueryEnabled = useMemo(() => {
        if (!clusterElementContext || currentNode) {
            return false;
        }

        if ((lookupDependsOnPaths as string[] | undefined)?.length && clusterElementLookupDependsOnValues) {
            return clusterElementLookupDependsOnValues.every(
                (dependencyValue) => dependencyValue !== undefined && dependencyValue !== null && dependencyValue !== ''
            );
        }

        return enabled;
    }, [currentNode, enabled, lookupDependsOnPaths, clusterElementLookupDependsOnValues, clusterElementContext]);

    const {data: clusterElementPropertyDynamicProperties, isLoading: isClusterElementPropertyDynamicPropertiesLoading} =
        useClusterElementPropertyDynamicPropertiesQuery(
            {
                clusterElementName: clusterElementContext?.clusterElementName || '',
                componentName: clusterElementContext?.componentName || '',
                componentVersion: clusterElementContext?.componentVersion || 0,
                connectionId: clusterElementContext?.connectionId,
                inputParameters: clusterElementInputParameters,
                lookupDependsOnPaths: (lookupDependsOnPaths as string[]) || [],
                propertyName: name || '',
            },
            {
                enabled: clusterElementContextQueryEnabled,
                queryKey: [
                    'clusterElementPropertyDynamicProperties',
                    {
                        clusterElementName: clusterElementContext?.clusterElementName,
                        componentName: clusterElementContext?.componentName,
                        componentVersion: clusterElementContext?.componentVersion,
                        connectionId: clusterElementContext?.connectionId,
                        lookupDependsOnValues: clusterElementLookupDependsOnValues,
                        propertyName: name,
                    },
                ],
            }
        );

    const clusterElementDynamicProperties =
        clusterElementPropertyDynamicProperties?.clusterElementPropertyDynamicProperties;

    // Update subProperties and track which key generated these properties
    useEffect(() => {
        if (properties) {
            setSubProperties(properties);

            setLastProcessedKey(lookupDependsOnValuesKey);
        } else if (clusterElementProperties) {
            setSubProperties(clusterElementProperties);

            setLastProcessedKey(lookupDependsOnValuesKey);
        } else if (clusterElementDynamicProperties && clusterElementDynamicProperties.length > 0) {
            setSubProperties(clusterElementDynamicProperties as PropertyAllType[]);

            setLastProcessedKey(lookupDependsOnValuesKey);
        } else {
            setSubProperties([]);
        }
    }, [properties, lookupDependsOnValuesKey, clusterElementProperties, clusterElementDynamicProperties]);

    const isPending = lookupDependsOnValuesKey !== lastProcessedKey;

    // Guard against primitive parameterValue (e.g., string) so bracket access doesn't hit prototype methods
    const paramValueObject = useMemo(
        () =>
            parameterValue && typeof parameterValue === 'object' && !Array.isArray(parameterValue)
                ? parameterValue
                : undefined,
        [parameterValue]
    );

    // In controlled mode, use the parent's calculatedPath as controlPath for children
    // so form fields register under the correct nested path (e.g., "fields.Id")
    const childControlPath = control ? path : controlPath;

    const hasLoadedData =
        (properties && properties.length > 0) ||
        (clusterElementProperties && clusterElementProperties.length > 0) ||
        (clusterElementDynamicProperties && clusterElementDynamicProperties.length > 0);

    const isAnyLoading =
        isLoading || isClusterElementPropertiesLoading || isClusterElementPropertyDynamicPropertiesLoading;
    const isAnyQueryEnabled = queryEnabled || clusterElementContextQueryEnabled;

    if ((isAnyLoading || isPending) && isAnyQueryEnabled && !hasLoadedData) {
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

    if (!subProperties.length) {
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
                        control={control}
                        controlPath={childControlPath}
                        dynamicPropertySource={name}
                        formState={formState}
                        key={`${property.name}_${index}_${lastProcessedKey}_property`}
                        objectName={control ? undefined : name}
                        operationName={currentOperationName}
                        parameterValue={defaultValue}
                        path={control ? undefined : path}
                        property={{
                            ...property,
                            defaultValue,
                        }}
                        toolsMode={toolsMode}
                    />
                );
            })}
        </ul>
    );
};

export default PropertyDynamicProperties;
