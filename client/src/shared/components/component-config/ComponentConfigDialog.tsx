import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ClusterElementProvider} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useClusterElementDefinitionQuery} from '@/shared/middleware/graphql';
import {ComponentOperationType} from '@/shared/middleware/graphql-types';
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

export interface ComponentConfigDialogTargetI {
    clusterElementName: string;
    componentName: string;
    componentVersion: number;
    description?: string | null;
    kind?: 'CLUSTER_ELEMENT' | 'TRIGGER';
    title?: string | null;
}

export interface ComponentConfigDialogInitialValuesI {
    connectionId?: string | null;
    parameters?: Record<string, unknown> | null;
}

export interface ComponentConfigDialogValuesI {
    connectionId: string | null;
    parameters: Record<string, unknown>;
}

interface ComponentConfigDialogPropsI {
    onClose: () => void;
    onSubmit: (values: ComponentConfigDialogValuesI) => Promise<void> | void;
    open: boolean;
    pending?: boolean;
    submitLabel?: string;
    picker?: ReactNode;
    excludedPropertyNames?: string[];
    target: ComponentConfigDialogTargetI | null;
    title?: string;
    description?: string;
    initialValues?: ComponentConfigDialogInitialValuesI;
    workspaceId: number;
}

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

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const {data: targetComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: target?.componentName ?? '',
            componentVersion: target?.componentVersion ?? 1,
        },
        target != null
    );

    const connectionTagsQueryResult = useGetConnectionTagsQuery();

    const formValues = form.watch();

    const {displayConditions: formDisplayConditions} = useFormDisplayConditions({
        componentName: target?.componentName,
        componentVersion: target?.componentVersion,
        enabled: target != null,
        operationName: target?.clusterElementName,
        operationType:
            target?.kind === 'TRIGGER' ? ComponentOperationType.Trigger : ComponentOperationType.ClusterElement,
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
        <WorkflowMockProvider>
            <Dialog onOpenChange={(nextOpen) => !nextOpen && onClose()} open={open}>
                <DialogContent size="md">
                    <DialogMain>
                        <DialogHeader description={resolvedDescription} title={resolvedTitle} />

                        <Form {...form}>
                            <form
                                className="flex min-h-0 flex-1 flex-col"
                                onSubmit={form.handleSubmit(handleFormSubmit)}
                            >
                                <DialogBody>
                                    {picker && <fieldset className="mb-4 space-y-4 border-0 p-0">{picker}</fieldset>}

                                    {picker && target == null ? (
                                        <p className="text-sm text-muted-foreground">
                                            Make a selection above to set its connection and properties.
                                        </p>
                                    ) : (
                                        <Tabs
                                            className="flex min-h-0 flex-1 flex-col"
                                            onValueChange={(value) =>
                                                setActiveTab(value as 'connection' | 'properties')
                                            }
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
                                                                            connectionsLoading ||
                                                                            connections.length === 0
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

                                                                {targetComponentDefinition && componentDefinitions && (
                                                                    <Tooltip>
                                                                        <TooltipTrigger asChild>
                                                                            <Button
                                                                                aria-label="Create new connection"
                                                                                icon={<PlusIcon />}
                                                                                onClick={() =>
                                                                                    setShowCreateConnection(true)
                                                                                }
                                                                                size="icon"
                                                                                type="button"
                                                                                variant="outline"
                                                                            />
                                                                        </TooltipTrigger>

                                                                        <TooltipContent>
                                                                            Create new connection
                                                                        </TooltipContent>
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

                                                {showCreateConnection &&
                                                    componentDefinitions &&
                                                    targetComponentDefinition && (
                                                        <ConnectionDialog
                                                            componentDefinition={targetComponentDefinition}
                                                            componentDefinitions={componentDefinitions}
                                                            connectionTagsQueryKey={ConnectionKeys.connectionTags}
                                                            connectionsQueryKey={ConnectionKeys.connections}
                                                            onClose={() => setShowCreateConnection(false)}
                                                            onConnectionCreate={(newId) => {
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
                                                                    connectionRequired:
                                                                        targetComponentDefinition?.connectionRequired,
                                                                    inputParameters:
                                                                        (formValues.parameters as Record<
                                                                            string,
                                                                            unknown
                                                                        >) ?? {},
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
                                                    <p className="text-sm text-muted-foreground">
                                                        No configurable parameters.
                                                    </p>
                                                )}
                                            </TabsContent>
                                        </Tabs>
                                    )}
                                </DialogBody>

                                <DialogFooter>
                                    <Button label="Cancel" onClick={onClose} type="button" variant="outline" />

                                    <Button
                                        disabled={submitDisabled}
                                        label={pending ? submitInProgressLabel : resolvedSubmitLabel}
                                        type="submit"
                                    />
                                </DialogFooter>
                            </form>
                        </Form>
                    </DialogMain>
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

    const excludedPropertyNamesKey = (excludedPropertyNames ?? []).join(',');

    const properties = useMemo(() => {
        const targetProperties = isTriggerTarget
            ? ((triggerDefinition?.properties ?? []) as unknown as PropertyAllType[])
            : ((clusterElementDefinition?.clusterElementDefinition?.properties ?? []) as unknown as PropertyAllType[]);

        if (!excludedPropertyNamesKey) {
            return targetProperties;
        }

        const excluded = new Set(excludedPropertyNamesKey.split(','));

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

    const defaultConnectionId = useMemo(() => {
        if (initialValues?.connectionId != null) {
            return initialValues.connectionId;
        }

        if (!connections || connections.length !== 1) {
            return undefined;
        }

        return connections[0]?.id != null ? String(connections[0].id) : undefined;
    }, [connections, initialValues?.connectionId]);

    const mergedDefaultParameters = useMemo(() => {
        const merged = initialValues?.parameters
            ? {...defaultParameters, ...initialValues.parameters}
            : defaultParameters;

        if (!excludedPropertyNamesKey) {
            return merged;
        }

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
