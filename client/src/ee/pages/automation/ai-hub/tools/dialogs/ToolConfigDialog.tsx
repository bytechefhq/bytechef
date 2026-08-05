import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ClusterElementProvider} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useClusterElementDefinitionQuery} from '@/shared/middleware/graphql';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PropertyAllType} from '@/shared/types';
import {PlusIcon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';

/**
 * The cluster element triple the dialog configures. Title + description are optional metadata used in the
 * dialog header; the (componentName, componentVersion, clusterElementName) tuple drives the cluster element
 * definition lookup that produces the property tree and connection candidates.
 */
export interface ToolConfigDialogTargetI {
    clusterElementName: string;
    componentName: string;
    componentVersion: number;
    description?: string | null;
    title?: string | null;
}

/**
 * Initial values to seed the form with. Used by the personal-agent edit flow to pre-fill the dialog with the
 * tool template's existing config; task attach passes both null and lets the form derive defaults from
 * the cluster element's property metadata.
 */
export interface ToolConfigDialogInitialValuesI {
    connectionId?: string | null;
    parameters?: Record<string, unknown> | null;
}

/**
 * Submit-time values handed to the caller's mutation. The dialog sanitizes empty strings to null + recursively
 * walks nested objects so the persisted parameters map matches what the workflow editor would produce — same
 * shape across every tool config surface so a future "edit in workflow editor" flow can read these back.
 */
export interface ToolConfigDialogValuesI {
    connectionId: string | null;
    parameters: Record<string, unknown>;
}

interface ToolConfigDialogPropsI {
    /** Called when the dialog should close (cancel button, escape, outside click, post-submit success). */
    onClose: () => void;
    /**
     * Caller-specific submit. Wired to whichever mutation the caller owns — attachAiHubTaskTool for the
     * task flow, updateAiHubPersonalAgentToolConfig for the agent-template flow. The dialog awaits this
     * promise before closing so the caller can short-circuit on validation failure.
     */
    onSubmit: (values: ToolConfigDialogValuesI) => Promise<void> | void;
    open: boolean;
    /** Whether the underlying mutation is in flight — disables the submit button + swaps the label. */
    pending?: boolean;
    /**
     * Override for the dialog header. Defaults to "Configure {target.title}" — task attach overrides
     * to "Attach {target.title} tool" so the verb matches the action being taken. Same for the submit button
     * label; the description override covers contexts where the default action-spec text doesn't fit.
     */
    submitLabel?: string;
    target: ToolConfigDialogTargetI | null;
    title?: string;
    description?: string;
    initialValues?: ToolConfigDialogInitialValuesI;
    workspaceId: number;
}

/**
 * Shared dialog for configuring a tool's connection + parameters. Used by both:
 * <ul>
 *   <li><b>Task attach</b> — picks a connection and pre-set parameters when the user attaches a tool
 *     to the active task. Submit fires the {@code attachAiHubTaskTool} mutation.</li>
 *   <li><b>Personal-agent template configure</b> — edits the per-tool config that gets carried into every
 *     task spawned from the agent. Submit fires the {@code updateAiHubPersonalAgentToolConfig} mutation.</li>
 * </ul>
 *
 * <p>
 * Layout mirrors the workflow node details panel: two tabs (Connection / Properties) with the connection
 * picker first because picking a connection is a prerequisite for many properties (dynamic property lists
 * depend on the connection to load their options). The Properties tab uses the workflow editor's
 * {@link Properties} renderer for type-aware inputs + dynamic option loading + inline validation — same
 * component the workflow editor uses, so the user experience is consistent.
 * </p>
 *
 * <p>
 * Form state lives entirely in {@code react-hook-form} via the shared {@code useToolConfigForm} hook below;
 * the caller's mutation gets handed sanitized values, never the raw form. Empty strings are coerced to null
 * recursively so the persisted parameter map doesn't carry placeholder empties (which the property renderer
 * emits for cleared fields and which the LLM treats as meaningful values otherwise).
 * </p>
 */
const ToolConfigDialog = ({
    description,
    initialValues,
    onClose,
    onSubmit,
    open,
    pending,
    submitLabel,
    target,
    title,
    workspaceId,
}: ToolConfigDialogPropsI) => {
    const [activeTab, setActiveTab] = useState<'connection' | 'properties'>('connection');
    const [showCreateConnection, setShowCreateConnection] = useState(false);

    const {connections, connectionsLoading, form, handleFormSubmit, properties, propertiesLoading} = useToolConfigForm({
        initialValues,
        onSubmit,
        target,
        workspaceId,
    });

    // Two queries feed the inline "Create new connection" affordance: the LIST so ConnectionDialog can render
    // its component picker (it pulls a connection-definition for whichever component is selected), and the
    // FULL definition for THIS component so the dialog opens straight to the configuration step with the right
    // form pre-filled — saves the user a "pick component" hop.
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const {data: targetComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: target?.componentName ?? '',
            componentVersion: target?.componentVersion ?? 1,
        },
        target != null
    );

    const connectionTagsQueryResult = useGetConnectionTagsQuery(workspaceId);

    const formValues = form.watch();

    const submitDisabled = pending || (connections.length > 1 && !formValues.connectionId);
    const resolvedTitle = title ?? `Configure ${target?.title || target?.clusterElementName || 'tool'}`;
    const resolvedDescription =
        description ??
        target?.description ??
        'Configure a connection and default parameters. The agent can override per call.';
    const resolvedSubmitLabel = submitLabel ?? 'Save';
    const submitInProgressLabel = `${resolvedSubmitLabel.replace(/e?$/, '')}ing…`;

    return (
        // `WorkflowMockProvider` satisfies the `useWorkflowEditor` context check that `Property` and its
        // `useProperty` hook require. The mock context is `{}` cast to the editor state shape — the
        // destructured mutations (deleteClusterElementParameterMutation, etc) end up undefined, which is
        // fine as long as the user only reads/edits parameter values and doesn't trigger the workflow
        // editor's save path. Our dialog submits via the caller's mutation directly, so those mutations are
        // never called. Without this wrap, `Property` throws on first render with "useWorkflowEditor must be
        // used within a WorkflowEditorProvider/WorkflowReadOnlyProviderContext".
        <WorkflowMockProvider>
            <Dialog onOpenChange={(nextOpen) => !nextOpen && onClose()} open={open}>
                <DialogContent className="flex max-h-[85vh] flex-col sm:max-w-2xl">
                    <DialogHeader>
                        <DialogTitle>{resolvedTitle}</DialogTitle>

                        <DialogDescription>{resolvedDescription}</DialogDescription>
                    </DialogHeader>

                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={form.handleSubmit(handleFormSubmit)}>
                            <Tabs
                                className="flex min-h-0 flex-1 flex-col"
                                onValueChange={(value) => setActiveTab(value as 'connection' | 'properties')}
                                value={activeTab}
                            >
                                <TabsList className="mb-3 grid w-full grid-cols-2">
                                    <TabsTrigger value="connection">Connection</TabsTrigger>

                                    <TabsTrigger value="properties">Properties</TabsTrigger>
                                </TabsList>

                                <TabsContent
                                    className="min-h-0 flex-1 space-y-4 overflow-y-auto pr-1"
                                    value="connection"
                                >
                                    <FormField
                                        control={form.control}
                                        name="connectionId"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Connection</FormLabel>

                                                <FormControl>
                                                    <Select
                                                        disabled={connectionsLoading || connections.length === 0}
                                                        onValueChange={field.onChange}
                                                        value={field.value ?? ''}
                                                    >
                                                        <SelectTrigger>
                                                            <SelectValue
                                                                placeholder={
                                                                    connections.length === 0
                                                                        ? 'No connections available'
                                                                        : 'Select a connection'
                                                                }
                                                            />
                                                        </SelectTrigger>

                                                        <SelectContent>
                                                            {connections.map((connection) => (
                                                                <SelectItem
                                                                    key={connection.id}
                                                                    value={String(connection.id)}
                                                                >
                                                                    {connection.name}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>
                                                </FormControl>

                                                <FormMessage />

                                                <p className="text-xs text-muted-foreground">
                                                    {connections.length === 0
                                                        ? `No connections for ${target?.componentName ?? 'this component'}. Create one to pre-bind this tool to a specific account.`
                                                        : connections.length === 1
                                                          ? 'Pre-selected — only one connection available for this component.'
                                                          : 'Pick the connection the agent will use when invoking this tool.'}
                                                </p>
                                            </FormItem>
                                        )}
                                    />

                                    {/*
                                     * Inline "Create new connection" affordance — opens the same ConnectionDialog
                                     * the workflow editor and the chat connect-flow use. After successful create,
                                     * react-query invalidation (driven by ConnectionDialog's connectionsQueryKey)
                                     * refetches the workspace connections list, the new connection appears in the
                                     * dropdown above, and the dialog stays open so the user can pick it without
                                     * losing their parameter form state.
                                     */}

                                    {targetComponentDefinition && componentDefinitions && (
                                        <Button
                                            icon={<PlusIcon className="size-3.5" />}
                                            label="Create new connection"
                                            onClick={() => setShowCreateConnection(true)}
                                            type="button"
                                            variant="outline"
                                        />
                                    )}

                                    {showCreateConnection && componentDefinitions && targetComponentDefinition && (
                                        <ConnectionDialog
                                            componentDefinition={targetComponentDefinition}
                                            componentDefinitions={componentDefinitions}
                                            connectionTagsQueryKey={ConnectionKeys.connectionTags(workspaceId)}
                                            connectionsQueryKey={ConnectionKeys.connections}
                                            onClose={() => setShowCreateConnection(false)}
                                            onConnectionCreate={(newId) => {
                                                // Auto-select the newly created connection so the user doesn't
                                                // have to scroll back to the dropdown to find it. The connections
                                                // list refetch is async, so the selection lands on a row that
                                                // doesn't exist yet — react-hook-form holds the value, and the
                                                // Select renders it once the list arrives.
                                                form.setValue('connectionId', String(newId));

                                                setShowCreateConnection(false);
                                            }}
                                            useCreateConnectionMutation={useCreateConnectionMutation}
                                            useGetConnectionTagsQuery={() => connectionTagsQueryResult}
                                        />
                                    )}
                                </TabsContent>

                                <TabsContent
                                    className="min-h-0 flex-1 space-y-4 overflow-y-auto pr-1"
                                    value="properties"
                                >
                                    {propertiesLoading ? (
                                        <p className="text-sm text-muted-foreground">Loading properties…</p>
                                    ) : properties.length > 0 ? (
                                        <fieldset className="space-y-4 border-0 p-0">
                                            {target && (
                                                <ClusterElementProvider
                                                    value={{
                                                        clusterElementName: target.clusterElementName,
                                                        componentName: target.componentName,
                                                        componentVersion: target.componentVersion,
                                                        connectionId:
                                                            formValues.connectionId != null
                                                                ? Number(formValues.connectionId)
                                                                : undefined,
                                                        inputParameters:
                                                            (formValues.parameters as Record<string, unknown>) ?? {},
                                                    }}
                                                >
                                                    <Properties
                                                        control={form.control}
                                                        controlPath="parameters"
                                                        formState={form.formState}
                                                        properties={properties}
                                                        toolsMode
                                                    />
                                                </ClusterElementProvider>
                                            )}
                                        </fieldset>
                                    ) : (
                                        <p className="text-sm text-muted-foreground">No configurable parameters.</p>
                                    )}
                                </TabsContent>
                            </Tabs>

                            <DialogFooter className="mt-4">
                                <Button label="Cancel" onClick={onClose} type="button" variant="outline" />

                                <Button
                                    disabled={submitDisabled}
                                    label={pending ? submitInProgressLabel : resolvedSubmitLabel}
                                    type="submit"
                                />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogContent>
            </Dialog>
        </WorkflowMockProvider>
    );
};

export default ToolConfigDialog;

interface UseToolConfigFormPropsI {
    initialValues?: ToolConfigDialogInitialValuesI;
    onSubmit: (values: ToolConfigDialogValuesI) => Promise<void> | void;
    target: ToolConfigDialogTargetI | null;
    workspaceId: number;
}

/**
 * Shared form-state hook for {@link ToolConfigDialog}. Handles:
 * <ul>
 *   <li><b>Property fetch</b> via {@code useClusterElementDefinitionQuery} — same query as the workflow
 *     editor uses for cluster-element details.</li>
 *   <li><b>Connection candidates</b> via {@code useGetWorkspaceConnectionsQuery} — auto-pre-selects the only
 *     candidate when there's exactly one (matches the workflow editor's UX).</li>
 *   <li><b>Default values</b> derived from each property's metadata (defaultValue / typeDefaultValue family),
 *     merged with the caller's {@code initialValues}.</li>
 *   <li><b>Sanitization</b> — empty strings → null, recursive walk through nested objects so the persisted
 *     parameters map doesn't carry placeholder empties.</li>
 * </ul>
 *
 * <p>
 * Returns the form-bound state plus the connection list and properties so the dialog can render its tabs
 * without re-querying. Decoupled from any specific mutation — the caller passes its own {@code onSubmit}
 * to handle the persistence layer.
 * </p>
 */
function useToolConfigForm({initialValues, onSubmit, target, workspaceId}: UseToolConfigFormPropsI) {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: clusterElementDefinition, isLoading: propertiesLoading} = useClusterElementDefinitionQuery(
        {
            clusterElementName: target?.clusterElementName ?? '',
            componentName: target?.componentName ?? '',
            componentVersion: target?.componentVersion ?? 1,
        },
        {enabled: target != null}
    );

    const {data: connections, isLoading: connectionsLoading} = useGetWorkspaceConnectionsQuery(
        {
            componentName: target?.componentName ?? '',
            environmentId: environmentId ?? undefined,
            id: workspaceId,
        },
        target != null
    );

    const properties = useMemo(() => {
        if (!clusterElementDefinition?.clusterElementDefinition?.properties) {
            return [];
        }

        return clusterElementDefinition.clusterElementDefinition.properties as unknown as PropertyAllType[];
    }, [clusterElementDefinition]);

    const defaultParameters = useMemo(() => {
        const seeded: Record<string, unknown> = {};

        for (const property of properties) {
            const propertyRecord = property as unknown as Record<string, unknown>;

            const resolvedDefault =
                propertyRecord.defaultValue ??
                propertyRecord.integerDefaultValue ??
                propertyRecord.numberDefaultValue ??
                propertyRecord.booleanDefaultValue ??
                propertyRecord.arrayDefaultValue ??
                propertyRecord.objectDefaultValue ??
                propertyRecord.dateDefaultValue ??
                propertyRecord.dateTimeDefaultValue ??
                propertyRecord.timeDefaultValue;

            if (property.name && resolvedDefault !== undefined && resolvedDefault !== null) {
                seeded[property.name] = resolvedDefault;
            }
        }

        return seeded;
    }, [properties]);

    // Auto-pick when there's exactly one connection. Zero connections leaves the picker blank so the user
    // gets routed through the connect flow; multiple connections requires explicit user choice (no silent
    // picking — wrong-target side effects are worse than an extra click).
    const defaultConnectionId = useMemo(() => {
        if (initialValues?.connectionId != null) {
            return initialValues.connectionId;
        }

        if (!connections || connections.length !== 1) {
            return undefined;
        }

        return connections[0]?.id != null ? String(connections[0].id) : undefined;
    }, [connections, initialValues?.connectionId]);

    // Merge caller-supplied initial parameters on top of the schema-derived defaults so the personal-agent
    // edit flow shows what the user previously configured (their values win over schema defaults). For the
    // task attach flow, initialValues is null so seeded defaults stand alone.
    const mergedDefaultParameters = useMemo(() => {
        if (!initialValues?.parameters) {
            return defaultParameters;
        }

        return {...defaultParameters, ...initialValues.parameters};
    }, [defaultParameters, initialValues?.parameters]);

    const form = useForm<{
        connectionId?: string;
        parameters: Record<string, unknown>;
    }>({
        defaultValues: {
            connectionId: defaultConnectionId,
            parameters: mergedDefaultParameters,
        },
    });

    useEffect(() => {
        form.reset({
            connectionId: defaultConnectionId,
            parameters: mergedDefaultParameters,
        });
    }, [defaultConnectionId, mergedDefaultParameters, form]);

    const sanitize = (record: Record<string, unknown>): Record<string, unknown> =>
        Object.fromEntries(
            Object.entries(record).map(([key, value]) => {
                if (value === '') {
                    return [key, null];
                }

                if (value && typeof value === 'object' && !Array.isArray(value)) {
                    return [key, sanitize(value as Record<string, unknown>)];
                }

                return [key, value];
            })
        );

    const handleFormSubmit = async (values: {connectionId?: string; parameters: Record<string, unknown>}) => {
        if (target == null) {
            return;
        }

        await onSubmit({
            connectionId: values.connectionId ? values.connectionId : null,
            parameters: sanitize(values.parameters ?? {}),
        });
    };

    return {
        connections: connections ?? [],
        connectionsLoading,
        form,
        handleFormSubmit,
        properties,
        propertiesLoading,
    };
}
