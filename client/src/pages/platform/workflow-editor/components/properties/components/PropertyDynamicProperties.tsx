import {convertNameToSnakeCase} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {PropertyDynamicPropertiesSkeleton} from '@/pages/platform/workflow-editor/components/WorkflowEditorSkeletons';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useClusterElementDynamicPropertiesQuery} from '@/shared/middleware/graphql';
import {
    useGetClusterElementNodeDynamicPropertiesQuery,
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
                clusterElementType: currentNode?.clusterElementType
                    ? convertNameToSnakeCase(currentNode.clusterElementType)
                    : '',
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

    const {data: workflowNodeDynamicProperties, isLoading} = useGetWorkflowNodeDynamicPropertiesQuery(
        queryOptions,
        Boolean(queryEnabled && !currentNode?.clusterElementType && !clusterElementContext)
    );

    const {data: clusterElementNodeDynamicProperties, isLoading: isClusterElementNodeDynamicPropertiesLoading} =
        useGetClusterElementNodeDynamicPropertiesQuery(
            clusterElementQueryOptions,
            Boolean(queryEnabled && currentNode?.clusterElementType && !clusterElementContext)
        );

    // Cluster element context fallback: resolve lookup dependencies from form values
    const clusterElementInputParameters = useMemo(() => {
        if (!clusterElementContext?.inputParameters) {
            return undefined;
        }

        const filteredParameters: Record<string, unknown> = {};

        for (const [parameterKey, parameterValue] of Object.entries(clusterElementContext.inputParameters)) {
            if (
                typeof parameterValue === 'string' &&
                (parameterValue.startsWith('=') || parameterValue.includes('${'))
            ) {
                continue;
            }

            filteredParameters[parameterKey] = parameterValue;
        }

        return filteredParameters;
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

    const {data: clusterElementDynamicProperties, isLoading: isClusterElementDynamicPropertiesLoading} =
        useClusterElementDynamicPropertiesQuery(
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
                    'clusterElementDynamicProperties',
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

    // Update subProperties and track which key generated these properties
    useEffect(() => {
        if (workflowNodeDynamicProperties) {
            setSubProperties(workflowNodeDynamicProperties);

            setLastProcessedKey(lookupDependsOnValuesKey);
        } else if (clusterElementNodeDynamicProperties) {
            setSubProperties(clusterElementNodeDynamicProperties);

            setLastProcessedKey(lookupDependsOnValuesKey);
        } else if (
            clusterElementDynamicProperties?.clusterElementDynamicProperties &&
            clusterElementDynamicProperties?.clusterElementDynamicProperties.length > 0
        ) {
            setSubProperties(clusterElementDynamicProperties?.clusterElementDynamicProperties as PropertyAllType[]);

            setLastProcessedKey(lookupDependsOnValuesKey);
        } else {
            setSubProperties([]);
        }
    }, [
        workflowNodeDynamicProperties,
        lookupDependsOnValuesKey,
        clusterElementNodeDynamicProperties,
        clusterElementDynamicProperties,
    ]);

    const isPending = lookupDependsOnValuesKey !== lastProcessedKey;

    // Guard against primitive parameterValue (e.g., string) so bracket access doesn't hit prototype methods
    const paramValueObject = useMemo(
        () =>
            parameterValue && typeof parameterValue === 'object' && !Array.isArray(parameterValue)
                ? parameterValue
                : undefined,
        [parameterValue]
    );

    const childControlPath = control ? path : controlPath;

    const hasLoadedData =
        (workflowNodeDynamicProperties && workflowNodeDynamicProperties.length > 0) ||
        (clusterElementNodeDynamicProperties && clusterElementNodeDynamicProperties.length > 0) ||
        (clusterElementDynamicProperties?.clusterElementDynamicProperties &&
            clusterElementDynamicProperties?.clusterElementDynamicProperties.length > 0);

    const isAnyLoading =
        isLoading || isClusterElementNodeDynamicPropertiesLoading || isClusterElementDynamicPropertiesLoading;

    const isAnyQueryEnabled = queryEnabled || clusterElementContextQueryEnabled;

    if ((isAnyLoading || isPending) && isAnyQueryEnabled && !hasLoadedData) {
        return <PropertyDynamicPropertiesSkeleton />;
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
