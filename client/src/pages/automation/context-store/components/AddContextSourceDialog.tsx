import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import RequiredMark from '@/components/RequiredMark';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
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
import {Textarea} from '@/components/ui/textarea';
import CadencePicker from '@/pages/automation/context-store/components/CadencePicker';
import IndexedFieldsEditor, {
    AvailableFieldI,
    IndexedFieldI,
} from '@/pages/automation/context-store/components/IndexedFieldsEditor';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ClusterElementProvider} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import TombstoneStrategySelect from '@/shared/components/TombstoneStrategySelect';
import {
    ContextStoreTombstoneStrategy,
    DataStreamCompatibleConnection,
    useClusterElementFieldsQuery,
    useContextStoresQuery,
    useCreateContextStoreSourceMutation,
    useDataStreamCompatibleConnectionsQuery,
} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetClusterElementDefinitionQuery} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {PackageIcon} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';
import {Control, FieldValues, FormState, useForm, useWatch} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';
import {useNavigate} from 'react-router-dom';

interface AddContextSourceDialogPropsI {
    /**
     * Optional store id to pre-select. When supplied (typical for the single-store detail page that opens this
     * dialog), the picker is hidden and the new source is created under that store. When omitted, the user picks
     * from the workspace's stores.
     */
    contextStoreId?: string;
    trigger?: ReactNode;
}

interface SourceParametersFormI {
    parameters: Record<string, unknown>;
}

const STEP_LABELS = ['Connection', 'Record shape', 'Cadence', 'Review'];

const AddContextSourceDialog = ({contextStoreId: presetContextStoreId, trigger}: AddContextSourceDialogPropsI) => {
    const [open, setOpen] = useState(false);
    const [step, setStep] = useState(0);
    const [name, setName] = useState('');
    // Two-stage Step 0 picker: pick the source-compatible component first, then a connection for that component.
    // Mirrors the MCP component dialog flow.
    const [selectedComponentName, setSelectedComponentName] = useState<string | null>(null);
    const [selectedConnection, setSelectedConnection] = useState<DataStreamCompatibleConnection | null>(null);
    const [contextStoreId, setContextStoreId] = useState<string>(presetContextStoreId ?? '');
    // Source absorbed the former Entity layer in Phase 2 — record-shape fields are inline on the dialog now.
    const [entityName, setEntityName] = useState('');
    const [description, setDescription] = useState('');
    const [idField, setIdField] = useState('');
    const [indexedFields, setIndexedFields] = useState<IndexedFieldI[]>([]);
    const [cadence, setCadence] = useState('@daily');
    const [fullReplaceCadence, setFullReplaceCadence] = useState('');
    const [tombstoneStrategy, setContextStoreTombstoneStrategy] = useState<ContextStoreTombstoneStrategy>(
        ContextStoreTombstoneStrategy.PeriodicFullReplace
    );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();
    const navigate = useNavigate();

    const {
        control,
        formState,
        getValues,
        reset: resetForm,
    } = useForm<SourceParametersFormI>({
        defaultValues: {parameters: {}},
    });

    const watchedParameters = useWatch({control, name: 'parameters'});

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

    const requiredSourcePropertyNames = useMemo(
        () =>
            sourceProperties
                .filter((property) => property.required && property.name)
                .map((property) => property.name as string),
        [sourceProperties]
    );

    const requiredSourcePropertiesReady = useMemo(() => {
        if (requiredSourcePropertyNames.length === 0) {
            return true;
        }

        const params = (watchedParameters ?? {}) as Record<string, unknown>;

        return requiredSourcePropertyNames.every((propertyName) => {
            const value = params[propertyName];

            return value !== undefined && value !== null && value !== '';
        });
    }, [requiredSourcePropertyNames, watchedParameters]);

    // Gate the fields query on step 1+ AND on the cluster element definition having loaded. Otherwise there's a
    // race when the user picks a connection on step 0: `sourceProperties` is briefly empty (definition still loading),
    // `requiredSourcePropertiesReady` evaluates to true on an empty required-names list, and the server-side resolver
    // NPEs on the missing baseId / tableId. The user shouldn't see the fields query fire until they're filling in the
    // record shape on step 1 anyway.
    const {data: fieldsData} = useClusterElementFieldsQuery(
        {
            clusterElementName: sourceClusterElementName ?? '',
            componentName: selectedConnection?.componentName ?? '',
            componentVersion: selectedConnection?.componentVersion ?? 1,
            connectionId: selectedConnection?.id ?? '',
            inputParameters: (watchedParameters ?? {}) as Record<string, unknown>,
        },
        {
            enabled:
                step >= 1 &&
                !!sourceClusterElementName &&
                !!selectedConnection &&
                !!sourceClusterElementDefinition &&
                requiredSourcePropertiesReady,
        }
    );

    const availableFields: AvailableFieldI[] = useMemo(
        () => fieldsData?.clusterElementFields ?? [],
        [fieldsData?.clusterElementFields]
    );

    const hasAvailableFields = availableFields.length > 0;

    const connections = useMemo(
        () =>
            (connectionsData?.dataStreamCompatibleConnections ?? []).filter(
                (connection): connection is NonNullable<typeof connection> => connection !== null
            ),
        [connectionsData?.dataStreamCompatibleConnections]
    );

    // Connection-bounded component catalog: derive from the connections we know about rather than the full component
    // definitions list. Keeps the picker honest — components without a usable connection can't be picked, which
    // matches the downstream contract anyway (the connection drives the actual sync).
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({actionDefinitions: true}, open);

    const availableComponents = useMemo(() => {
        const byName = new Map<string, {description?: string; icon?: string; name: string; title?: string}>();

        for (const connection of connections) {
            if (byName.has(connection.componentName)) {
                continue;
            }

            const definition = componentDefinitions?.find((component) => component.name === connection.componentName);

            byName.set(connection.componentName, {
                description: definition?.description,
                icon: definition?.icon,
                name: connection.componentName,
                title: definition?.title,
            });
        }

        return Array.from(byName.values()).sort((firstComponent, secondComponent) =>
            (firstComponent.title ?? firstComponent.name).localeCompare(secondComponent.title ?? secondComponent.name)
        );
    }, [connections, componentDefinitions]);

    const connectionsForSelectedComponent = useMemo(
        () =>
            selectedComponentName === null
                ? []
                : connections.filter((connection) => connection.componentName === selectedComponentName),
        [connections, selectedComponentName]
    );

    const createMutation = useCreateContextStoreSourceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['contextStoreSources']});
            setOpen(false);
            resetState();

            if (contextStoreId) {
                navigate(`/automation/context-stores/${contextStoreId}`);
            } else {
                navigate('/automation/context-stores');
            }
        },
    });

    const {data: contextStoresData} = useContextStoresQuery(
        {
            environmentId: String(currentEnvironmentId),
            workspaceId: String(currentWorkspaceId),
        },
        {enabled: open}
    );

    const existingContextStores = contextStoresData?.contextStores ?? [];

    const resetState = () => {
        setStep(0);
        setName('');
        setSelectedComponentName(null);
        setSelectedConnection(null);
        setContextStoreId(presetContextStoreId ?? '');
        setEntityName('');
        setDescription('');
        setIdField('');
        setIndexedFields([]);
        setCadence('@daily');
        setFullReplaceCadence('');
        setContextStoreTombstoneStrategy(ContextStoreTombstoneStrategy.PeriodicFullReplace);
        resetForm({parameters: {}});
    };

    const handleOpenChange = (nextOpen: boolean) => {
        setOpen(nextOpen);

        if (!nextOpen) {
            resetState();
        }
    };

    const canProceed = useMemo(() => {
        if (step === 0) return selectedConnection !== null && name.trim() !== '' && contextStoreId !== '';

        if (step === 1) {
            return entityName.trim() !== '' && idField.trim() !== '' && indexedFields.length > 0;
        }

        if (step === 2) return cadence.trim() !== '';

        return true;
    }, [step, selectedConnection, name, contextStoreId, entityName, idField, indexedFields, cadence]);

    const handleSubmit = () => {
        if (!selectedConnection || contextStoreId === '') return;

        const sourceParameters = getValues('parameters') ?? {};

        const indexedFieldsMap: Record<string, string> = {};

        for (const field of indexedFields) {
            if (field.name.trim() !== '') {
                indexedFieldsMap[field.name] = field.type;
            }
        }

        const fullReplaceCadencePayload = fullReplaceCadence.trim() !== '' ? fullReplaceCadence : undefined;

        // Tombstone strategy always sent; server validates compatibility with the chosen cadence shape. The records
        // backend (Postgres or ClickHouse) is a deployment-wide opt-in, so no per-source backend field is sent.
        createMutation.mutate({
            input: {
                cadence,
                connectionId: selectedConnection.id,
                contextStoreId,
                description: description || undefined,
                entityName,
                environmentId: String(currentEnvironmentId),
                fullReplaceCadence: fullReplaceCadencePayload,
                idField,
                indexedFields: indexedFieldsMap,
                name,
                parameters: sourceParameters,
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

            <DialogContent className="sm:max-w-[700px]">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <fieldset className="flex flex-col space-y-1 border-0 p-0">
                        <DialogTitle>Add Context Source</DialogTitle>

                        <DialogDescription>
                            Step {step + 1} of {STEP_LABELS.length}: {STEP_LABELS[step]}
                        </DialogDescription>
                    </fieldset>

                    <DialogCloseButton />
                </DialogHeader>

                <fieldset className="space-y-4 border-0 p-0 py-4">
                    {step === 0 && (
                        <fieldset className="space-y-4 border-0 p-0">
                            {!presetContextStoreId && (
                                <fieldset className="space-y-2 border-0 p-0">
                                    <Label htmlFor="contextStoreId">Context Store</Label>

                                    {existingContextStores.length === 0 ? (
                                        <p
                                            className="rounded-md border border-amber-200 bg-surface-warning-secondary p-2 text-xs text-content-warning-primary"
                                            data-testid="no-context-stores-warning"
                                        >
                                            No Context Stores exist in this workspace at the active environment. Create
                                            one on the Context Stores page before adding sources.
                                        </p>
                                    ) : (
                                        <Select onValueChange={setContextStoreId} value={contextStoreId}>
                                            <SelectTrigger data-testid="context-store-select" id="contextStoreId">
                                                <SelectValue placeholder="Select a Context Store..." />
                                            </SelectTrigger>

                                            <SelectContent>
                                                {existingContextStores.map((store) => (
                                                    <SelectItem key={store.id} value={store.id}>
                                                        {store.name}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    )}
                                </fieldset>
                            )}

                            <fieldset className="space-y-2 border-0 p-0">
                                <Label htmlFor="sourceName">Source Name</Label>

                                <Input
                                    id="sourceName"
                                    onChange={(event) => setName(event.target.value)}
                                    placeholder="My HubSpot CRM"
                                    value={name}
                                />
                            </fieldset>

                            {selectedComponentName === null ? (
                                <fieldset className="space-y-2 border-0 p-0">
                                    <Label>Pick a Component</Label>

                                    {connectionsLoading ? (
                                        <p className="flex items-center gap-2 text-sm text-muted-foreground">
                                            <LoadingIcon /> Loading components...
                                        </p>
                                    ) : availableComponents.length === 0 ? (
                                        <p className="text-sm text-muted-foreground">
                                            No data-stream-compatible connections found in this workspace. Create one on
                                            the Connections page before adding a source.
                                        </p>
                                    ) : (
                                        <div
                                            className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3"
                                            data-testid="component-grid"
                                        >
                                            {availableComponents.map((component) => (
                                                <Card
                                                    className="cursor-pointer transition-shadow hover:shadow-md"
                                                    data-testid={`component-option-${component.name}`}
                                                    key={component.name}
                                                    onClick={() => setSelectedComponentName(component.name)}
                                                >
                                                    <CardHeader className="pb-2 text-center">
                                                        <div className="mx-auto mb-2">
                                                            {component.icon ? (
                                                                <InlineSVG className="size-12" src={component.icon} />
                                                            ) : (
                                                                <PackageIcon className="size-12 text-content-neutral-tertiary" />
                                                            )}
                                                        </div>

                                                        <CardTitle className="text-sm">
                                                            {component.title || component.name}
                                                        </CardTitle>
                                                    </CardHeader>

                                                    {component.description && (
                                                        <CardContent className="pt-0">
                                                            <CardDescription className="line-clamp-2 text-center text-xs">
                                                                {component.description}
                                                            </CardDescription>
                                                        </CardContent>
                                                    )}
                                                </Card>
                                            ))}
                                        </div>
                                    )}
                                </fieldset>
                            ) : (
                                <fieldset className="space-y-2 border-0 p-0">
                                    <div className="flex items-center justify-between">
                                        <Label>Pick a Connection</Label>

                                        <button
                                            className="text-xs text-muted-foreground hover:text-foreground"
                                            data-testid="change-component"
                                            onClick={() => {
                                                setSelectedComponentName(null);
                                                setSelectedConnection(null);
                                            }}
                                            type="button"
                                        >
                                            Change component
                                        </button>
                                    </div>

                                    {connectionsForSelectedComponent.length === 0 ? (
                                        <p className="text-sm text-muted-foreground">
                                            No connections for this component. Create one on the Connections page.
                                        </p>
                                    ) : (
                                        <fieldset
                                            className="max-h-64 space-y-1 overflow-y-auto rounded-md border border-border p-2"
                                            data-testid="connection-list"
                                        >
                                            {connectionsForSelectedComponent.map((connection) => {
                                                const isSelected = selectedConnection?.id === connection.id;

                                                return (
                                                    <button
                                                        aria-pressed={isSelected}
                                                        className={
                                                            isSelected
                                                                ? 'flex w-full items-center justify-between rounded-md bg-surface-brand-secondary px-3 py-2 text-left'
                                                                : 'flex w-full items-center justify-between rounded-md px-3 py-2 text-left hover:bg-muted'
                                                        }
                                                        data-testid={`connection-option-${connection.id}`}
                                                        key={connection.id}
                                                        onClick={() => setSelectedConnection(connection)}
                                                        type="button"
                                                    >
                                                        <fieldset className="border-0 p-0">
                                                            <p className="text-sm font-medium">{connection.name}</p>

                                                            <p className="text-xs text-muted-foreground">
                                                                {connection.componentName}v{connection.componentVersion}
                                                            </p>
                                                        </fieldset>
                                                    </button>
                                                );
                                            })}
                                        </fieldset>
                                    )}
                                </fieldset>
                            )}
                        </fieldset>
                    )}

                    {step === 1 && selectedConnection && (
                        <fieldset className="space-y-4 border-0 p-0" data-testid="record-shape-step">
                            <fieldset className="space-y-1 border-0 p-0">
                                <Label htmlFor="entityName">
                                    Entity Name
                                    <RequiredMark />
                                </Label>

                                <Input
                                    id="entityName"
                                    onChange={(event) => setEntityName(event.target.value)}
                                    placeholder="contacts"
                                    value={entityName}
                                />
                            </fieldset>

                            <fieldset className="space-y-1 border-0 p-0">
                                <Label htmlFor="description">Description</Label>

                                <Textarea
                                    id="description"
                                    onChange={(event) => setDescription(event.target.value)}
                                    placeholder="Optional"
                                    value={description}
                                />
                            </fieldset>

                            {sourceProperties.length > 0 && (
                                <fieldset className="space-y-2 border-0 p-0">
                                    <Label>Source Configuration</Label>

                                    <WorkflowMockProvider>
                                        <ClusterElementProvider
                                            value={{
                                                clusterElementName: sourceClusterElementName ?? '',
                                                componentName: selectedConnection.componentName,
                                                componentVersion: selectedConnection.componentVersion,
                                                connectionId: selectedConnection.id
                                                    ? Number(selectedConnection.id)
                                                    : undefined,
                                                inputParameters: (watchedParameters ?? {}) as Record<string, unknown>,
                                            }}
                                        >
                                            <Properties
                                                control={control as unknown as Control<FieldValues>}
                                                controlPath="parameters"
                                                customClassName="p-0"
                                                formState={formState as unknown as FormState<FieldValues>}
                                                hideFromAi={true}
                                                properties={sourceProperties}
                                            />
                                        </ClusterElementProvider>
                                    </WorkflowMockProvider>
                                </fieldset>
                            )}

                            <fieldset className="space-y-1 border-0 p-0">
                                <Label htmlFor="idField">ID Field</Label>

                                {hasAvailableFields ? (
                                    <Select onValueChange={setIdField} value={idField}>
                                        <SelectTrigger aria-label="ID Field" id="idField">
                                            <SelectValue placeholder="Select a field..." />
                                        </SelectTrigger>

                                        <SelectContent>
                                            {availableFields.map((field) => (
                                                <SelectItem key={field.name} value={field.name}>
                                                    {field.label || field.name}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                ) : (
                                    <Input
                                        id="idField"
                                        onChange={(event) => setIdField(event.target.value)}
                                        placeholder="id"
                                        value={idField}
                                    />
                                )}
                            </fieldset>

                            <fieldset className="space-y-1 border-0 p-0">
                                <Label>Indexed Fields</Label>

                                <IndexedFieldsEditor
                                    availableFields={availableFields}
                                    fields={indexedFields}
                                    onChange={setIndexedFields}
                                />
                            </fieldset>
                        </fieldset>
                    )}

                    {step === 2 && (
                        <fieldset className="space-y-4 border-0 p-0" data-testid="cadence-step">
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
                                onChange={(value) =>
                                    setContextStoreTombstoneStrategy(value as ContextStoreTombstoneStrategy)
                                }
                                testId="tombstone-strategy-select"
                                value={tombstoneStrategy}
                            />
                        </fieldset>
                    )}

                    {step === 3 && (
                        <fieldset className="space-y-3 border-0 p-0 text-sm" data-testid="review-step">
                            <p>
                                <strong>Context Store:</strong>

                                {` ${existingContextStores.find((store) => store.id === contextStoreId)?.name ?? ''}`}
                            </p>

                            <p>
                                <strong>Name:</strong> {name}
                            </p>

                            <p>
                                <strong>Connection:</strong>

                                {` ${selectedConnection?.name ?? ''} (${selectedConnection?.componentName ?? ''} v${selectedConnection?.componentVersion ?? ''})`}
                            </p>

                            <p>
                                <strong>Entity:</strong> {entityName}
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

export default AddContextSourceDialog;
