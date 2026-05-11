import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import CadencePicker from '@/pages/automation/context-store/components/CadencePicker';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import TombstoneStrategySelect from '@/shared/components/TombstoneStrategySelect';
import {
    DataStreamCompatibleConnection,
    TombstoneStrategy,
    useClusterElementFieldsQuery,
    useCreateKnowledgeBaseSourceMutation,
    useDataStreamCompatibleConnectionsQuery,
} from '@/shared/middleware/graphql';
import {useGetClusterElementDefinitionQuery} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useMemo, useState} from 'react';
import {Control, FieldValues, FormState, useForm} from 'react-hook-form';

interface AddKnowledgeBaseSourceDialogPropsI {
    knowledgeBaseId: string;
    trigger?: ReactNode;
}

interface SourceParametersFormI {
    sourceParameters: Record<string, unknown>;
}

const STEP_LABELS = ['Connection', 'Source Configuration', 'Metadata Tags', 'Cadence', 'Review'];

const AddKnowledgeBaseSourceDialog = ({knowledgeBaseId, trigger}: AddKnowledgeBaseSourceDialogPropsI) => {
    const [open, setOpen] = useState(false);
    const [step, setStep] = useState(0);
    const [name, setName] = useState('');
    const [selectedConnection, setSelectedConnection] = useState<DataStreamCompatibleConnection | null>(null);
    const [cadence, setCadence] = useState('@daily');
    const [selectedMetadataFields, setSelectedMetadataFields] = useState<string[]>([]);
    // Phase 17b paired-cadence picker. Empty string = single-trigger (MVP); any other value = paired. Wire format
    // mirrors `cadence` so the same CadencePicker component can render both.
    const [fullReplaceCadence, setFullReplaceCadence] = useState('');
    const [tombstoneStrategy, setTombstoneStrategy] = useState<TombstoneStrategy>(
        TombstoneStrategy.PeriodicFullReplace
    );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {
        control,
        formState,
        getValues,
        reset: resetForm,
    } = useForm<SourceParametersFormI>({
        defaultValues: {sourceParameters: {}},
    });

    const {data: connectionsData, isLoading: connectionsLoading} = useDataStreamCompatibleConnectionsQuery(
        {
            environmentId: String(currentEnvironmentId),
            workspaceId: String(currentWorkspaceId),
        },
        {enabled: open}
    );

    const {data: sourceComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: selectedConnection?.componentName ?? '',
            componentVersion: selectedConnection?.componentVersion ?? 1,
        },
        !!selectedConnection
    );

    const sourceClusterElementName = useMemo(() => {
        if (!sourceComponentDefinition?.clusterElements) return undefined;

        const sourceElement = sourceComponentDefinition.clusterElements.find((element) => element.type === 'SOURCE');

        return sourceElement?.name;
    }, [sourceComponentDefinition]);

    const {data: sourceClusterElementDefinition} = useGetClusterElementDefinitionQuery(
        {
            clusterElementName: sourceClusterElementName ?? '',
            clusterElementType: 'source',
            componentName: selectedConnection?.componentName ?? '',
            componentVersion: selectedConnection?.componentVersion ?? 1,
        },
        !!sourceClusterElementName && !!selectedConnection
    );

    const sourceProperties = useMemo(
        () => (sourceClusterElementDefinition?.properties as PropertyAllType[] | undefined) ?? [],
        [sourceClusterElementDefinition?.properties]
    );

    // Probe the source reader for its discoverable metadata fields once both the cluster element name and the
    // step-1 source parameters are settled. The query short-circuits client-side until both inputs exist, so
    // hitting Next from step 0 doesn't fire it.
    const {data: metadataFieldsData, isLoading: metadataFieldsLoading} = useClusterElementFieldsQuery(
        {
            clusterElementName: sourceClusterElementName ?? '',
            componentName: selectedConnection?.componentName ?? '',
            componentVersion: selectedConnection?.componentVersion ?? 1,
            connectionId: selectedConnection?.id ?? null,
            inputParameters: getValues('sourceParameters') as Record<string, unknown>,
        },
        {enabled: !!sourceClusterElementName && !!selectedConnection && step >= 2}
    );

    const availableMetadataFields = useMemo(
        () => metadataFieldsData?.clusterElementFields ?? [],
        [metadataFieldsData?.clusterElementFields]
    );

    const connections = useMemo(
        () =>
            (connectionsData?.dataStreamCompatibleConnections ?? []).filter(
                (connection): connection is NonNullable<typeof connection> => connection !== null
            ),
        [connectionsData?.dataStreamCompatibleConnections]
    );

    const resetState = () => {
        setStep(0);
        setName('');
        setSelectedConnection(null);
        setCadence('@daily');
        setSelectedMetadataFields([]);
        setFullReplaceCadence('');
        setTombstoneStrategy(TombstoneStrategy.PeriodicFullReplace);
        resetForm({sourceParameters: {}});
    };

    const toggleMetadataField = (fieldName: string) => {
        setSelectedMetadataFields((current) =>
            current.includes(fieldName) ? current.filter((name) => name !== fieldName) : [...current, fieldName]
        );
    };

    const createMutation = useCreateKnowledgeBaseSourceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseSources']});
            setOpen(false);
            resetState();
        },
    });

    const handleOpenChange = (nextOpen: boolean) => {
        setOpen(nextOpen);

        if (!nextOpen) {
            resetState();
        }
    };

    const canProceed = useMemo(() => {
        if (step === 0) return selectedConnection !== null && name.trim() !== '';
        if (step === 1) return true;
        // Step 2 (Metadata Tags) is always proceedable — leaving the selection empty preserves MVP behavior
        // (every reader-emitted field becomes a tag), so users can skip it.
        if (step === 2) return true;
        if (step === 3) return cadence.trim() !== '';

        return true;
    }, [step, selectedConnection, name, cadence]);

    const handleSubmit = () => {
        if (!selectedConnection) return;

        const collectedParameters = getValues('sourceParameters') ?? {};

        // The server stores metadataFields as JSONB shaped {fields: [...]}; null preserves MVP "every key becomes
        // a tag" behavior. Sending an empty array would still trip the malformed-whitelist guard server-side, but
        // omitting the key entirely keeps the wire payload smaller.
        const metadataFieldsPayload = selectedMetadataFields.length > 0 ? {fields: selectedMetadataFields} : undefined;

        // Empty fullReplaceCadence stays single-trigger MVP behavior; the GraphQL input drops the field.
        const fullReplaceCadencePayload = fullReplaceCadence.trim() !== '' ? fullReplaceCadence : undefined;

        // Tombstone strategy is only meaningful when paired-cadence is configured. For single-trigger sources the
        // default PERIODIC_FULL_REPLACE is harmless (the workflow has no full-replace trigger anyway), so we still
        // send the user's choice — the server validates compatibility.
        createMutation.mutate({
            input: {
                cadence,
                connectionId: selectedConnection.id,
                environmentId: String(currentEnvironmentId),
                fullReplaceCadence: fullReplaceCadencePayload,
                knowledgeBaseId,
                metadataFields: metadataFieldsPayload,
                name,
                parameters: collectedParameters,
                sourceComponentName: selectedConnection.componentName,
                sourceComponentVersion: selectedConnection.componentVersion,
                tombstoneStrategy,
                workspaceId: String(currentWorkspaceId),
            },
        });
    };

    const cadenceDisplay = fullReplaceCadence ? `${cadence} (paired with full re-sync ${fullReplaceCadence})` : cadence;

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogTrigger asChild>{trigger || <Button>Add Source</Button>}</DialogTrigger>

            <DialogContent className="sm:max-w-[600px]">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <fieldset className="flex flex-col space-y-1 border-0 p-0">
                        <DialogTitle>Add Knowledge Base Source</DialogTitle>

                        <DialogDescription>
                            Step {step + 1} of {STEP_LABELS.length}: {STEP_LABELS[step]}
                        </DialogDescription>
                    </fieldset>

                    <DialogCloseButton />
                </DialogHeader>

                <fieldset className="space-y-4 border-0 p-0 py-4">
                    {step === 0 && (
                        <fieldset className="space-y-4 border-0 p-0">
                            <fieldset className="space-y-2 border-0 p-0">
                                <Label htmlFor="kbsSourceName">Source Name</Label>

                                <Input
                                    id="kbsSourceName"
                                    onChange={(event) => setName(event.target.value)}
                                    placeholder="My S3 docs"
                                    value={name}
                                />
                            </fieldset>

                            <fieldset className="space-y-2 border-0 p-0">
                                <Label>Pick a Connection</Label>

                                {connectionsLoading ? (
                                    <p className="text-sm text-muted-foreground">Loading connections...</p>
                                ) : connections.length === 0 ? (
                                    <p className="text-sm text-muted-foreground">
                                        No data-stream-compatible connections found.
                                    </p>
                                ) : (
                                    <fieldset
                                        className="max-h-64 space-y-1 overflow-y-auto rounded-md border border-border p-2"
                                        data-testid="kbs-connection-list"
                                    >
                                        {connections.map((connection) => {
                                            const isSelected = selectedConnection?.id === connection.id;

                                            return (
                                                <button
                                                    aria-pressed={isSelected}
                                                    className={
                                                        isSelected
                                                            ? 'flex w-full items-center justify-between rounded-md bg-blue-50 px-3 py-2 text-left'
                                                            : 'flex w-full items-center justify-between rounded-md px-3 py-2 text-left hover:bg-muted'
                                                    }
                                                    data-testid={`kbs-connection-option-${connection.id}`}
                                                    key={connection.id}
                                                    onClick={() => setSelectedConnection(connection)}
                                                    type="button"
                                                >
                                                    <fieldset className="border-0 p-0">
                                                        <p className="text-sm font-medium">{connection.name}</p>

                                                        <p className="text-xs text-muted-foreground">
                                                            {connection.componentName} v{connection.componentVersion}
                                                        </p>
                                                    </fieldset>
                                                </button>
                                            );
                                        })}
                                    </fieldset>
                                )}
                            </fieldset>
                        </fieldset>
                    )}

                    {step === 1 &&
                        (sourceProperties.length > 0 ? (
                            <fieldset className="space-y-2 border-0 p-0" data-testid="kbs-source-configuration-step">
                                <Label>Source Configuration</Label>

                                <WorkflowMockProvider>
                                    <Properties
                                        control={control as unknown as Control<FieldValues>}
                                        controlPath="sourceParameters"
                                        customClassName="p-0"
                                        formState={formState as unknown as FormState<FieldValues>}
                                        hideFromAi={true}
                                        properties={sourceProperties}
                                    />
                                </WorkflowMockProvider>
                            </fieldset>
                        ) : (
                            <fieldset className="space-y-2 border-0 p-0" data-testid="kbs-source-configuration-empty">
                                <Label>Source Configuration</Label>

                                <p className="text-sm text-muted-foreground">
                                    No source configuration required for this reader.
                                </p>
                            </fieldset>
                        ))}

                    {step === 2 && (
                        <fieldset className="space-y-2 border-0 p-0" data-testid="kbs-metadata-fields-step">
                            <Label>Metadata Tags</Label>

                            <p className="text-xs text-muted-foreground">
                                Choose which record fields become document tags. Leave empty to keep the default
                                behavior of tagging every reader-emitted field.
                            </p>

                            {metadataFieldsLoading ? (
                                <p className="text-sm text-muted-foreground">Discovering available fields...</p>
                            ) : availableMetadataFields.length === 0 ? (
                                <p className="text-sm text-muted-foreground">
                                    This source does not advertise discoverable fields. All record fields will become
                                    tags.
                                </p>
                            ) : (
                                <fieldset
                                    className="max-h-64 space-y-1 overflow-y-auto rounded-md border border-border p-2"
                                    data-testid="kbs-metadata-fields-list"
                                >
                                    {availableMetadataFields.map((field) => {
                                        const isSelected = selectedMetadataFields.includes(field.name);

                                        return (
                                            <label
                                                className="flex w-full cursor-pointer items-center gap-2 rounded-md px-2 py-1 text-sm hover:bg-muted"
                                                key={field.name}
                                            >
                                                <input
                                                    checked={isSelected}
                                                    data-testid={`kbs-metadata-field-${field.name}`}
                                                    onChange={() => toggleMetadataField(field.name)}
                                                    type="checkbox"
                                                />

                                                <span className="font-medium">{field.label || field.name}</span>

                                                {field.label && field.label !== field.name && (
                                                    <span className="text-xs text-muted-foreground">{field.name}</span>
                                                )}

                                                {field.type && (
                                                    <span className="ml-auto text-xs text-muted-foreground">
                                                        {field.type}
                                                    </span>
                                                )}
                                            </label>
                                        );
                                    })}
                                </fieldset>
                            )}
                        </fieldset>
                    )}

                    {step === 3 && (
                        <fieldset className="space-y-4 border-0 p-0" data-testid="kbs-cadence-step">
                            <fieldset className="space-y-2 border-0 p-0">
                                <Label>Sync Cadence</Label>

                                <p className="text-xs text-muted-foreground">
                                    How often the source pulls fresh records.
                                </p>

                                <CadencePicker onChange={setCadence} value={cadence} />
                            </fieldset>

                            <fieldset className="space-y-2 border-0 p-0">
                                <Label>Full Re-sync Cadence (optional)</Label>

                                <p className="text-xs text-muted-foreground">
                                    Pair the sync cadence above with a less-frequent full re-sync that detects upstream
                                    deletions. Recommended for sources without a deletion event feed. Leave empty to
                                    keep single-trigger behavior.
                                </p>

                                <CadencePicker
                                    onChange={setFullReplaceCadence}
                                    value={fullReplaceCadence || '@manual'}
                                />

                                {fullReplaceCadence && (
                                    <button
                                        className="text-xs text-muted-foreground underline"
                                        onClick={() => setFullReplaceCadence('')}
                                        type="button"
                                    >
                                        Clear (single-trigger)
                                    </button>
                                )}
                            </fieldset>

                            <TombstoneStrategySelect
                                onChange={(value) => setTombstoneStrategy(value as TombstoneStrategy)}
                                testId="kbs-tombstone-strategy-select"
                                value={tombstoneStrategy}
                            />
                        </fieldset>
                    )}

                    {step === 4 && (
                        <fieldset className="space-y-3 border-0 p-0 text-sm" data-testid="kbs-review-step">
                            <p>
                                <strong>Name:</strong> {name}
                            </p>

                            <p>
                                <strong>Connection:</strong>

                                {` ${selectedConnection?.name ?? ''} (${selectedConnection?.componentName ?? ''} v${selectedConnection?.componentVersion ?? ''})`}
                            </p>

                            <p>
                                <strong>Source Configuration:</strong>

                                {sourceProperties.length === 0
                                    ? ' (none required)'
                                    : ` ${JSON.stringify(getValues('sourceParameters') ?? {})}`}
                            </p>

                            <p>
                                <strong>Metadata Tags:</strong>

                                {selectedMetadataFields.length === 0
                                    ? ' (every field becomes a tag — MVP default)'
                                    : ` ${selectedMetadataFields.join(', ')}`}
                            </p>

                            <p>
                                <strong>Cadence:</strong> {cadenceDisplay}
                            </p>

                            <p>
                                <strong>Tombstone Strategy:</strong> {tombstoneStrategy}
                            </p>
                        </fieldset>
                    )}
                </fieldset>

                <DialogFooter className="flex justify-between">
                    <Button disabled={step === 0} onClick={() => setStep(Math.max(0, step - 1))} variant="ghost">
                        Back
                    </Button>

                    {step < STEP_LABELS.length - 1 ? (
                        <Button
                            disabled={!canProceed}
                            onClick={() => setStep(Math.min(STEP_LABELS.length - 1, step + 1))}
                        >
                            Next
                        </Button>
                    ) : (
                        <Button disabled={createMutation.isPending} onClick={handleSubmit}>
                            {createMutation.isPending ? 'Creating...' : 'Create Source'}
                        </Button>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AddKnowledgeBaseSourceDialog;
