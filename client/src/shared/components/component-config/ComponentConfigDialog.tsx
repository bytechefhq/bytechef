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
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
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
import {useGetTriggerDefinitionQuery} from '@/shared/queries/platform/triggerDefinitions.queries';
import useFormDisplayConditions from '@/shared/queries/platform/useFormDisplayConditions';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PropertyAllType} from '@/shared/types';
import {PlusIcon} from 'lucide-react';
import {type ReactNode, useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';

/**
 * The operation the dialog configures. Title + description are optional metadata used in the dialog header;
 * the (componentName, componentVersion, clusterElementName) tuple drives the definition lookup that produces
 * the property tree and connection candidates.
 *
 * `kind` selects which definition that name is looked up as. It defaults to CLUSTER_ELEMENT, which every tool
 * surface uses — a tool is addressed as a TOOLS cluster element, and the server serves one even for components
 * that register no cluster element of their own. TRIGGER is what the agent's channel rows configure, a channel
 * being a trigger node rather than a cluster element. Only the definition query differs — the connection tab,
 * property renderer and submit shape are identical, which is the whole reason these share one dialog.
 */
export interface ComponentConfigDialogTargetI {
    /** The operation name: a cluster element name, or a trigger name when `kind` is TRIGGER. */
    clusterElementName: string;
    componentName: string;
    componentVersion: number;
    description?: string | null;
    kind?: 'CLUSTER_ELEMENT' | 'TRIGGER';
    title?: string | null;
}

/**
 * Initial values to seed the form with. Used by the task edit flow to pre-fill the dialog with the
 * tool template's existing config; chat attach passes both null and lets the form derive defaults from
 * the cluster element's property metadata.
 */
export interface ComponentConfigDialogInitialValuesI {
    connectionId?: string | null;
    parameters?: Record<string, unknown> | null;
}

/**
 * Submit-time values handed to the caller's mutation. The dialog sanitizes empty strings to null + recursively
 * walks nested objects so the persisted parameters map matches what the workflow editor would produce — same
 * shape across every tool config surface so a future "edit in workflow editor" flow can read these back.
 */
export interface ComponentConfigDialogValuesI {
    connectionId: string | null;
    parameters: Record<string, unknown>;
}

interface ComponentConfigDialogPropsI {
    /** Called when the dialog should close (cancel button, escape, outside click, post-submit success). */
    onClose: () => void;
    /**
     * Caller-specific submit. Wired to whichever mutation the caller owns — an attach mutation when a
     * tool is first attached, an update mutation when an attached tool is reconfigured. The dialog
     * awaits this promise before closing so the caller can short-circuit on validation failure.
     */
    onSubmit: (values: ComponentConfigDialogValuesI) => Promise<void> | void;
    open: boolean;
    /** Whether the underlying mutation is in flight — disables the submit button + swaps the label. */
    pending?: boolean;
    /**
     * Override for the dialog header. Defaults to "Configure {target.title}" — attach flows override
     * to "Attach {target.title} tool" so the verb matches the action being taken. Same for the submit button
     * label; the description override covers contexts where the default action-spec text doesn't fit.
     */
    submitLabel?: string;
    /**
     * Selection UI rendered above the tabs, for the add flows where the target is chosen inside the dialog
     * rather than by the row that opened it. While `target` is null the tabs are withheld — there is no
     * definition yet to draw a connection list or a property tree from — and the submit button stays disabled.
     * Picking hands back a resolved `target`, at which point the form re-seeds from that target's own property
     * defaults. This is what lets an add flow set properties up front instead of forcing add-then-configure.
     */
    picker?: ReactNode;
    /**
     * Property names the caller owns outside the tabs and does not want a second editor for. The agent's
     * model dialog excludes `model`, which it picks in its own combobox: the model cluster element declares
     * `model` among its own properties, so without this the Properties tab showed a second Model field
     * carrying the schema default rather than the chosen model.
     *
     * <p>
     * Excluded names are withheld from the rendered tree AND from the submitted parameters — including a
     * value inherited through {@code initialValues} — since anything left in the map would keep writing back
     * over the value the caller owns.
     * </p>
     */
    excludedPropertyNames?: string[];
    target: ComponentConfigDialogTargetI | null;
    title?: string;
    description?: string;
    initialValues?: ComponentConfigDialogInitialValuesI;
    workspaceId: number;
}

/**
 * Shared dialog for configuring a tool's connection + parameters. Used by both:
 * <ul>
 *   <li><b>Chat attach</b> — picks a connection and pre-set parameters when the user attaches a tool
 *     to the active chat. Submit fires the {@code attachAiHubChatTool} mutation.</li>
 *   <li><b>Agent element configure</b> — edits the per-element config carried by an AI Agent's tool,
 *     model, and channel cards. Submit fires that card's own element mutation.</li>
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
 * Form state lives entirely in {@code react-hook-form} via the shared {@code useComponentConfigForm} hook below;
 * the caller's mutation gets handed sanitized values, never the raw form. Empty strings are coerced to null
 * recursively so the persisted parameter map doesn't carry placeholder empties (which the property renderer
 * emits for cleared fields and which the LLM treats as meaningful values otherwise).
 * </p>
 */
const ComponentConfigDialog = ({
    description,
    excludedPropertyNames,
    initialValues,
    onClose,
    onSubmit,
    open,
    pending,
    picker,
    submitLabel,
    target,
    title,
    workspaceId,
}: ComponentConfigDialogPropsI) => {
    const [activeTab, setActiveTab] = useState<'connection' | 'properties'>('connection');
    const [showCreateConnection, setShowCreateConnection] = useState(false);

    const {connections, connectionsLoading, form, handleFormSubmit, properties, propertiesLoading} =
        useComponentConfigForm({
            excludedPropertyNames,
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

    // With a picker in play the dialog opens before anything is selected; submitting then would persist a
    // row pointing at no operation.
    // Conditions are evaluated against the form's live values, so choosing a body content type hides the other
    // body properties as it would in the workflow editor. Undefined until evaluated, which keeps them all visible.
    const formDisplayConditions = useFormDisplayConditions({
        componentName: target?.componentName,
        componentVersion: target?.componentVersion,
        enabled: target != null,
        operationName: target?.clusterElementName,
        operationType: target?.kind === 'TRIGGER' ? 'TRIGGER' : 'CLUSTER_ELEMENT',
        parameters: formValues.parameters ?? {},
    });

    const submitDisabled = pending || target == null || (connections.length > 1 && !formValues.connectionId);
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
                            {picker && <fieldset className="mb-4 space-y-4 border-0 p-0">{picker}</fieldset>}

                            {picker && target == null ? (
                                <p className="text-sm text-muted-foreground">
                                    Make a selection above to set its connection and properties.
                                </p>
                            ) : (
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

                                                    <div className="flex items-center gap-2">
                                                        <FormControl>
                                                            <Select
                                                                disabled={
                                                                    connectionsLoading || connections.length === 0
                                                                }
                                                                onValueChange={field.onChange}
                                                                value={field.value ?? ''}
                                                            >
                                                                <SelectTrigger className="flex-1">
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

                                                        {/* Icon-only and beside the select rather than a labelled
                                                            button below it: creating a connection is an escape
                                                            hatch from picking one, so it belongs on the control it
                                                            escapes, not competing with the dialog's own actions. */}

                                                        {targetComponentDefinition && componentDefinitions && (
                                                            <Tooltip>
                                                                <TooltipTrigger asChild>
                                                                    <Button
                                                                        aria-label="Create new connection"
                                                                        icon={<PlusIcon />}
                                                                        onClick={() => setShowCreateConnection(true)}
                                                                        size="icon"
                                                                        type="button"
                                                                        variant="outline"
                                                                    />
                                                                </TooltipTrigger>

                                                                <TooltipContent>Create new connection</TooltipContent>
                                                            </Tooltip>
                                                        )}
                                                    </div>

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
                                         * The + beside the select opens the same ConnectionDialog the workflow editor
                                         * and the chat connect-flow use. After a successful create, react-query
                                         * invalidation (driven by ConnectionDialog's connectionsQueryKey) refetches
                                         * the workspace connections list, the new connection appears in the dropdown,
                                         * and this dialog stays open so the user can pick it without losing their
                                         * parameter form state.
                                         */}

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
                                                                (formValues.parameters as Record<string, unknown>) ??
                                                                {},
                                                        }}
                                                    >
                                                        <Properties
                                                            control={form.control}
                                                            controlPath="parameters"
                                                            formDisplayConditions={formDisplayConditions}
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
                            )}

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

export default ComponentConfigDialog;

interface UseComponentConfigFormPropsI {
    excludedPropertyNames?: string[];
    initialValues?: ComponentConfigDialogInitialValuesI;
    onSubmit: (values: ComponentConfigDialogValuesI) => Promise<void> | void;
    target: ComponentConfigDialogTargetI | null;
    workspaceId: number;
}

/**
 * Shared form-state hook for {@link ComponentConfigDialog}. Handles:
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
function useComponentConfigForm({
    excludedPropertyNames,
    initialValues,
    onSubmit,
    target,
    workspaceId,
}: UseComponentConfigFormPropsI) {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const isTriggerTarget = target?.kind === 'TRIGGER';

    const {data: clusterElementDefinition, isLoading: clusterElementLoading} = useClusterElementDefinitionQuery(
        {
            clusterElementName: target?.clusterElementName ?? '',
            componentName: target?.componentName ?? '',
            componentVersion: target?.componentVersion ?? 1,
        },
        {enabled: target != null && !isTriggerTarget}
    );

    const {data: triggerDefinition, isLoading: triggerLoading} = useGetTriggerDefinitionQuery(
        {
            componentName: target?.componentName ?? '',
            componentVersion: target?.componentVersion ?? 1,
            triggerName: target?.clusterElementName ?? '',
        },
        target != null && isTriggerTarget
    );

    const propertiesLoading = isTriggerTarget ? triggerLoading : clusterElementLoading;

    const {data: connections, isLoading: connectionsLoading} = useGetWorkspaceConnectionsQuery(
        {
            componentName: target?.componentName ?? '',
            environmentId: environmentId ?? undefined,
            id: workspaceId,
        },
        target != null
    );

    // Compared by content rather than by array identity: callers pass this inline, and a fresh array each
    // render would recompute the memos below, whose changing identity re-runs the form reset effect — an
    // infinite render loop rather than a wasted render.
    const excludedPropertyNamesKey = (excludedPropertyNames ?? []).join(',');

    const properties = useMemo(() => {
        const targetProperties = isTriggerTarget
            ? ((triggerDefinition?.properties ?? []) as unknown as PropertyAllType[])
            : ((clusterElementDefinition?.clusterElementDefinition?.properties ?? []) as unknown as PropertyAllType[]);

        if (!excludedPropertyNamesKey) {
            return targetProperties;
        }

        const excluded = new Set(excludedPropertyNamesKey.split(','));

        // A nameless property cannot be addressed by name, so it can never be one of the excluded.
        return targetProperties.filter((property) => !property.name || !excluded.has(property.name));
    }, [clusterElementDefinition, excludedPropertyNamesKey, isTriggerTarget, triggerDefinition]);

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

    // Merge caller-supplied initial parameters on top of the schema-derived defaults so the task
    // edit flow shows what the user previously configured (their values win over schema defaults). For the
    // chat attach flow, initialValues is null so seeded defaults stand alone.
    const mergedDefaultParameters = useMemo(() => {
        const merged = initialValues?.parameters
            ? {...defaultParameters, ...initialValues.parameters}
            : defaultParameters;

        if (!excludedPropertyNamesKey) {
            return merged;
        }

        // `defaultParameters` is already clear of these — it is derived from the filtered property list — but
        // `initialValues` is the caller's own previously saved map and can still carry one.
        const excluded = new Set(excludedPropertyNamesKey.split(','));

        return Object.fromEntries(Object.entries(merged).filter(([name]) => !excluded.has(name)));
    }, [defaultParameters, excludedPropertyNamesKey, initialValues?.parameters]);

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
