import useDebouncedSave from '@/pages/automation/data-syncs/hooks/useDebouncedSave';
import {elementKindKey, findElement} from '@/pages/automation/data-syncs/utils/dataSyncElements';
import invalidateDataSyncQueries from '@/pages/automation/data-syncs/utils/invalidateDataSyncQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {convertNameToSnakeCase} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {
    ClusterElementDefinitionDocument,
    type ClusterElementDefinitionQuery,
    type ClusterElementDefinitionQueryVariables,
    ComponentOperationType,
    DataSync,
    DataSyncElementKind,
    useClusterElementDefinitionQuery,
    useSetDataSyncElementMutation,
    useUpdateDataSyncElementMutation,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import useFormDisplayConditions from '@/shared/queries/platform/useFormDisplayConditions';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useForm} from 'react-hook-form';
import {toast} from 'sonner';

interface UseDataSyncElementStepProps {
    dataSync: Pick<DataSync, 'elements' | 'id'>;
    kind: DataSyncElementKind;
}

interface ElementFormValuesI {
    connectionId: string | null;
    parameters: Record<string, unknown>;
}

export function defaultParametersOf(properties: PropertyAllType[]): Record<string, unknown> {
    const seeded: Record<string, unknown> = {};

    for (const property of properties) {
        const propertyRecord = property as unknown as Record<string, unknown>;

        const resolvedDefault =
            propertyRecord.defaultValue ??
            propertyRecord.integerDefaultValue ??
            propertyRecord.numberDefaultValue ??
            propertyRecord.booleanDefaultValue ??
            propertyRecord.arrayDefaultValue ??
            propertyRecord.objectDefaultValue;

        if (property.name && resolvedDefault !== undefined && resolvedDefault !== null) {
            seeded[property.name] = resolvedDefault;
        }
    }

    return seeded;
}

export default function useDataSyncElementStep({dataSync, kind}: UseDataSyncElementStepProps) {
    const element = findElement(dataSync, kind);

    const [selectedComponentName, setSelectedComponentName] = useState<string>(element?.componentName ?? '');
    const [selectedOperationName, setSelectedOperationName] = useState<string>(element?.operationName ?? '');

    const lastSeededValuesRef = useRef<string>('');

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const clusterElementTypeKey = convertNameToSnakeCase(elementKindKey(kind));

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        clusterElementDefinitions: true,
    });

    const candidateDefinitions = useMemo(
        () =>
            (componentDefinitions ?? []).filter(
                (definition) => (definition.clusterElementsCount?.[clusterElementTypeKey] ?? 0) > 0
            ),
        [clusterElementTypeKey, componentDefinitions]
    );

    const selectedDefinitionVersion =
        element && element.componentName === selectedComponentName
            ? element.componentVersion
            : (candidateDefinitions.find((definition) => definition.name === selectedComponentName)?.version ?? 1);

    const {data: selectedComponentDefinition} = useGetComponentDefinitionQuery(
        {componentName: selectedComponentName, componentVersion: selectedDefinitionVersion},
        !!selectedComponentName
    );

    const hasConnection = selectedComponentDefinition?.connection !== undefined;

    const connectionRequired = selectedComponentDefinition?.connectionRequired;

    const operations = useMemo(
        () =>
            (selectedComponentDefinition?.clusterElements ?? []).filter(
                (clusterElement) => clusterElement.type === clusterElementTypeKey
            ),
        [clusterElementTypeKey, selectedComponentDefinition?.clusterElements]
    );

    const {data: clusterElementDefinitionData} = useClusterElementDefinitionQuery(
        {
            clusterElementName: selectedOperationName,
            componentName: selectedComponentName,
            componentVersion: selectedDefinitionVersion,
        },
        {enabled: !!selectedComponentName && !!selectedOperationName}
    );

    const properties = useMemo(
        () =>
            (clusterElementDefinitionData?.clusterElementDefinition?.properties ?? []) as unknown as PropertyAllType[],
        [clusterElementDefinitionData]
    );

    const {data: connections} = useGetWorkspaceConnectionsQuery(
        {
            componentName: element?.componentName ?? '',
            environmentId: currentEnvironmentId ?? undefined,
            id: currentWorkspaceId!,
        },
        !!element && currentWorkspaceId != null
    );

    const form = useForm<ElementFormValuesI>({
        defaultValues: {
            connectionId: element?.connectionId != null ? String(element.connectionId) : null,
            parameters: (element?.parameters ?? {}) as Record<string, unknown>,
        },
    });

    const formValues = form.watch();

    const {displayConditions: formDisplayConditions} = useFormDisplayConditions({
        componentName: element?.componentName,
        componentVersion: element?.componentVersion,
        enabled: !!element,
        operationName: element?.operationName,
        operationType: ComponentOperationType.ClusterElement,
        parameters: formValues.parameters ?? {},
    });

    const setElementMutation = useSetDataSyncElementMutation({
        onSuccess: () => invalidateDataSyncQueries(queryClient),
    });

    const updateElementMutation = useUpdateDataSyncElementMutation({
        onSuccess: () => invalidateDataSyncQueries(queryClient),
    });

    const handleComponentChange = useCallback((componentName: string) => {
        setSelectedComponentName(componentName);
        setSelectedOperationName('');
    }, []);

    const handleOperationChange = useCallback(
        (operationName: string) => {
            const previousOperationName = selectedOperationName;

            setSelectedOperationName(operationName);

            const clusterElementDefinitionVariables: ClusterElementDefinitionQueryVariables = {
                clusterElementName: operationName,
                componentName: selectedComponentName,
                componentVersion: selectedDefinitionVersion,
            };

            queryClient
                .fetchQuery({
                    queryFn: fetcher<ClusterElementDefinitionQuery, ClusterElementDefinitionQueryVariables>(
                        ClusterElementDefinitionDocument,
                        clusterElementDefinitionVariables
                    ),
                    queryKey: ['clusterElementDefinition', clusterElementDefinitionVariables],
                })
                .then((definition) => {
                    setElementMutation.mutate(
                        {
                            input: {
                                componentName: selectedComponentName,
                                componentVersion: selectedDefinitionVersion,
                                connectionId: null,
                                dataSyncId: dataSync.id,
                                kind,
                                operationName,
                                parameters: defaultParametersOf(
                                    (definition.clusterElementDefinition?.properties ??
                                        []) as unknown as PropertyAllType[]
                                ),
                            },
                        },
                        {
                            onSuccess: (data) => {
                                form.reset({
                                    connectionId: null,
                                    parameters: (data.setDataSyncElement.parameters ?? {}) as Record<string, unknown>,
                                });
                            },
                        }
                    );
                })
                .catch(() => {
                    setSelectedOperationName(previousOperationName);

                    toast.error('Failed to load the operation schema.');
                });
        },
        [
            dataSync.id,
            form,
            kind,
            queryClient,
            selectedComponentName,
            selectedDefinitionVersion,
            selectedOperationName,
            setElementMutation,
        ]
    );

    const handleConnectionChange = useCallback(
        (connectionId: string | null) => {
            form.setValue('connectionId', connectionId, {shouldDirty: true});
        },
        [form]
    );

    const candidateValuesToSave = element
        ? {
              connectionId: formValues.connectionId,
              id: element.id,
              parameters: formValues.parameters ?? {},
          }
        : null;

    const valuesToSave =
        candidateValuesToSave && JSON.stringify(candidateValuesToSave) === lastSeededValuesRef.current
            ? null
            : candidateValuesToSave;

    useDebouncedSave(valuesToSave, (values) => {
        if (values) {
            updateElementMutation.mutate({input: values});
        }
    });

    useEffect(() => {
        if (!element) {
            setSelectedComponentName('');
            setSelectedOperationName('');

            form.reset({connectionId: null, parameters: {}});

            lastSeededValuesRef.current = '';

            return;
        }

        setSelectedComponentName(element.componentName);
        setSelectedOperationName(element.operationName);

        const seededConnectionId = element.connectionId != null ? String(element.connectionId) : null;
        const seededParameters = (element.parameters ?? {}) as Record<string, unknown>;

        form.reset({
            connectionId: seededConnectionId,
            parameters: seededParameters,
        });

        lastSeededValuesRef.current = JSON.stringify({
            connectionId: seededConnectionId,
            id: element.id,
            parameters: seededParameters,
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [element?.id, element?.componentName, element?.operationName]);

    return {
        candidateDefinitions,
        connectionRequired,
        connections: connections ?? [],
        element,
        form,
        formDisplayConditions,
        handleComponentChange,
        handleConnectionChange,
        handleOperationChange,
        hasConnection,
        operations,
        properties,
        selectedComponentName,
        selectedOperationName,
    };
}
