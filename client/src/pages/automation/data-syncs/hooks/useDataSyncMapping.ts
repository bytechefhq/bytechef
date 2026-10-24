import useDebouncedSave from '@/pages/automation/data-syncs/hooks/useDebouncedSave';
import {
    DATA_SYNC_TASK_NODE_NAME,
    PROCESSOR_COMPONENT_NAME,
    PROCESSOR_COMPONENT_VERSION,
    PROCESSOR_OPERATION_NAME,
    elementNodeName,
    findElement,
} from '@/pages/automation/data-syncs/utils/dataSyncElements';
import invalidateDataSyncQueries from '@/pages/automation/data-syncs/utils/invalidateDataSyncQueries';
import {
    DataSync,
    DataSyncElementKind,
    useSetDataSyncElementMutation,
    useUpdateDataSyncElementMutation,
} from '@/shared/middleware/graphql';
import {WorkflowNodeOptionApi} from '@/shared/middleware/platform/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {toast} from 'sonner';

export interface FieldMappingI {
    destinationField: string;
    sourceField: string;
}

interface FieldOptionI {
    label: string;
    value: string;
}

interface UseDataSyncMappingProps {
    dataSync: Pick<DataSync, 'draftWorkflowId' | 'elements' | 'id'>;
}

export default function useDataSyncMapping({dataSync}: UseDataSyncMappingProps) {
    const processor = findElement(dataSync, DataSyncElementKind.Processor);

    const [mappings, setMappings] = useState<FieldMappingI[]>(
        ((processor?.parameters as {mappings?: FieldMappingI[]} | undefined)?.mappings ?? []).map((mapping) => ({
            destinationField: mapping.destinationField ?? '',
            sourceField: mapping.sourceField ?? '',
        }))
    );
    const [sourceOptions, setSourceOptions] = useState<FieldOptionI[]>([]);
    const [destinationOptions, setDestinationOptions] = useState<FieldOptionI[]>([]);
    const [autoMapping, setAutoMapping] = useState(false);
    const [optionsLoading, setOptionsLoading] = useState(!!processor);
    const [optionsLoadFailed, setOptionsLoadFailed] = useState(false);

    const processorCreationRequestedRef = useRef(false);
    const lastProcessorIdRef = useRef(processor?.id);

    if (processor?.id !== lastProcessorIdRef.current) {
        lastProcessorIdRef.current = processor?.id;

        if (processor) {
            setOptionsLoading(true);
        }
    }

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const hasSourceAndDestination =
        !!findElement(dataSync, DataSyncElementKind.Source) && !!findElement(dataSync, DataSyncElementKind.Destination);

    const setElementMutation = useSetDataSyncElementMutation({
        onSuccess: () => invalidateDataSyncQueries(queryClient),
    });

    const updateElementMutation = useUpdateDataSyncElementMutation({
        onSuccess: () => invalidateDataSyncQueries(queryClient),
    });

    const loadOptions = useCallback(
        async (propertyName: string): Promise<FieldOptionI[]> => {
            const options = await new WorkflowNodeOptionApi().getClusterElementNodeOptions({
                clusterElementType: 'PROCESSOR',
                clusterElementWorkflowNodeName: elementNodeName(DataSyncElementKind.Processor),
                environmentId: currentEnvironmentId,
                id: dataSync.draftWorkflowId,
                propertyName,
                workflowNodeName: DATA_SYNC_TASK_NODE_NAME,
            });

            return options
                .filter((option) => option.value != null && option.value !== '')
                .map((option) => ({label: String(option.label ?? option.value), value: String(option.value)}));
        },
        [currentEnvironmentId, dataSync.draftWorkflowId]
    );

    const handleAutoMap = useCallback(async () => {
        setAutoMapping(true);

        try {
            const [sourceFields, destinationFields] = await Promise.all([
                loadOptions('mappings[0].sourceField'),
                loadOptions('mappings[0].destinationField'),
            ]);

            const destinationValues = new Set(destinationFields.map((option) => option.value));

            const matched = sourceFields
                .filter((option) => destinationValues.has(option.value))
                .map((option) => ({destinationField: option.value, sourceField: option.value}));

            if (matched.length === 0) {
                toast.info('No matching fields found between source and destination');

                return;
            }

            setMappings(matched);
        } catch {
            toast.error('Auto-map failed');
        } finally {
            setAutoMapping(false);
        }
    }, [loadOptions]);

    const handleAddMapping = useCallback(
        () => setMappings((current) => [...current, {destinationField: '', sourceField: ''}]),
        []
    );

    const handleRemoveMapping = useCallback(
        (index: number) => setMappings((current) => current.filter((_, mappingIndex) => mappingIndex !== index)),
        []
    );

    const handleMappingChange = useCallback(
        (index: number, patch: Partial<FieldMappingI>) =>
            setMappings((current) =>
                current.map((mapping, mappingIndex) => (mappingIndex === index ? {...mapping, ...patch} : mapping))
            ),
        []
    );

    const valuesToSave = useMemo(
        () =>
            processor
                ? {
                      connectionId: null,
                      id: processor.id,
                      parameters: {
                          mappings: mappings.filter((mapping) => mapping.sourceField && mapping.destinationField),
                      },
                  }
                : null,
        [mappings, processor]
    );

    useDebouncedSave(valuesToSave, (values) => {
        if (values) {
            updateElementMutation.mutate({input: values});
        }
    });

    useEffect(() => {
        if (processor || !hasSourceAndDestination || processorCreationRequestedRef.current) {
            return;
        }

        processorCreationRequestedRef.current = true;

        setElementMutation.mutate(
            {
                input: {
                    componentName: PROCESSOR_COMPONENT_NAME,
                    componentVersion: PROCESSOR_COMPONENT_VERSION,
                    connectionId: null,
                    dataSyncId: dataSync.id,
                    kind: DataSyncElementKind.Processor,
                    operationName: PROCESSOR_OPERATION_NAME,
                    parameters: {mappings: []},
                },
            },
            {
                onError: () => {
                    processorCreationRequestedRef.current = false;
                },
            }
        );
    }, [dataSync.id, hasSourceAndDestination, processor, setElementMutation]);

    useEffect(() => {
        if (!processor) {
            return;
        }

        let cancelled = false;

        setOptionsLoading(true);
        setOptionsLoadFailed(false);

        Promise.all([loadOptions('mappings[0].sourceField'), loadOptions('mappings[0].destinationField')])
            .then(([sourceFields, destinationFields]) => {
                if (!cancelled) {
                    setSourceOptions(sourceFields);
                    setDestinationOptions(destinationFields);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setOptionsLoadFailed(true);

                    toast.error('Could not load the source and destination fields');
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setOptionsLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [loadOptions, processor?.id]);

    return {
        autoMapping,
        destinationOptions,
        handleAddMapping,
        handleAutoMap,
        handleMappingChange,
        handleRemoveMapping,
        hasSourceAndDestination,
        mappings,
        optionsLoadFailed,
        optionsLoading,
        processor,
        sourceOptions,
    };
}
