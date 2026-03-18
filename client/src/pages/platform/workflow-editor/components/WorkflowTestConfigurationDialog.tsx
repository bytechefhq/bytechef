import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {ConnectionI, useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {
    ComponentConnection,
    Workflow,
    WorkflowInput,
    WorkflowTestConfiguration,
    WorkflowTestConfigurationConnection,
} from '@/shared/middleware/platform/configuration';
import {useSaveWorkflowTestConfigurationMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {PropertyAllType} from '@/shared/types';
import * as Portal from '@radix-ui/react-portal';
import {useQueryClient} from '@tanstack/react-query';
import {InfoIcon, PlusIcon} from 'lucide-react';
import {Dispatch, SetStateAction, useState} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

interface WorkflowTestConfigurationDialogProps {
    onClose: () => void;
    workflow: Workflow;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

interface WorkflowTestConfigurationFormFieldProps {
    componentConnection: ComponentConnection;
    connectionDialogAllowed: boolean;
    connections: ConnectionI[];
    form: UseFormReturn<WorkflowTestConfiguration>;
    groupedIndices?: number[];
    index: number;
    setComponentConnection: Dispatch<SetStateAction<ComponentConnection | undefined>>;
    setShowNewConnectionDialog: Dispatch<SetStateAction<boolean>>;
}

const WorkflowTestConfigurationFormField = ({
    componentConnection,
    connectionDialogAllowed,
    connections,
    form,
    groupedIndices,
    index,
    setComponentConnection,
    setShowNewConnectionDialog,
}: WorkflowTestConfigurationFormFieldProps) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: componentConnection.componentName,
        componentVersion: componentConnection.componentVersion,
    });

    return (
        <div key={index}>
            <FormField
                control={form.control}
                name={`connections.${index}.connectionId`}
                render={({field}) => {
                    return (
                        <FormItem>
                            <FormLabel className="flex items-center">
                                {componentDefinition?.icon && (
                                    <InlineSVG className="size-4 flex-none" src={componentDefinition.icon} />
                                )}

                                <span className="ml-1">{componentDefinition?.title} Connection</span>

                                {groupedIndices ? (
                                    <span className="ml-0.5 text-xs text-content-neutral-secondary">
                                        (applies to {groupedIndices.length} nodes)
                                    </span>
                                ) : (
                                    <span className="ml-0.5 text-xs text-content-neutral-secondary">
                                        {`(${componentConnection.workflowNodeName} - ${componentConnection.key})`}
                                    </span>
                                )}
                            </FormLabel>

                            <Select
                                onValueChange={(value) => {
                                    field.onChange(value);

                                    if (groupedIndices) {
                                        for (const groupedIndex of groupedIndices) {
                                            if (groupedIndex !== index) {
                                                form.setValue(
                                                    `connections.${groupedIndex}.connectionId`,
                                                    Number(value)
                                                );
                                            }
                                        }
                                    }
                                }}
                                value={field.value ? field.value.toString() : undefined}
                            >
                                <FormControl>
                                    <div className="flex space-x-2">
                                        <SelectTrigger>
                                            <SelectValue placeholder="Choose Connection..." />
                                        </SelectTrigger>

                                        {connectionDialogAllowed && (
                                            <Button
                                                className="mt-auto p-2"
                                                icon={<PlusIcon className="size-5" />}
                                                onClick={() => {
                                                    setComponentConnection(componentConnection);
                                                    setShowNewConnectionDialog(true);
                                                }}
                                                title="Create a new connection"
                                                type="button"
                                                variant="outline"
                                            />
                                        )}
                                    </div>
                                </FormControl>

                                <SelectContent>
                                    {connections &&
                                        connections
                                            .filter(
                                                (connection) =>
                                                    connection.componentName === componentConnection.componentName
                                            )
                                            .map((connection) => (
                                                <SelectItem key={connection.id} value={connection.id!.toString()}>
                                                    <div className="flex items-center">
                                                        <span className="mr-1">{connection.name}</span>

                                                        <span className="text-xs text-gray-500">
                                                            {connection?.tags?.map((tag) => tag.name).join(', ')}
                                                        </span>

                                                        <EnvironmentBadge environmentId={connection.environmentId!} />
                                                    </div>
                                                </SelectItem>
                                            ))}
                                </SelectContent>
                            </Select>

                            <FormMessage />
                        </FormItem>
                    );
                }}
                rules={{
                    required: componentConnection.required,
                }}
            />
        </div>
    );
};

const WorkflowTestConfigurationDialog = ({
    onClose,
    workflow,
    workflowTestConfiguration,
}: WorkflowTestConfigurationDialogProps) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);
    const [componentConnection, setComponentConnection] = useState<ComponentConnection | undefined>();
    const [groupConnections, setGroupConnections] = useState(false);

    const connectionDialogAllowed = useWorkflowNodeDetailsPanelStore((state) => state.connectionDialogAllowed);

    const {
        ConnectionKeys,
        useCreateConnectionMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionTagsQuery,
        useGetConnectionsQuery,
    } = useWorkflowEditor();

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const componentConnections: ComponentConnection[] = [
        ...(workflow?.triggers ?? []),
        ...(workflow?.tasks ?? []),
    ].flatMap((operation) => (operation.connections ? operation.connections : []));

    const workflowTestConfigurationConnections = componentConnections.map((componentConnection) => {
        const workflowTestConfigurationConnection = (workflowTestConfiguration?.connections ?? []).find(
            (curWorkflowTestConfigurationConfiguration) =>
                curWorkflowTestConfigurationConfiguration.workflowNodeName === componentConnection.workflowNodeName &&
                curWorkflowTestConfigurationConfiguration.workflowConnectionKey === componentConnection.key
        );

        return (
            workflowTestConfigurationConnection ??
            ({
                workflowConnectionKey: componentConnection.key,
                workflowNodeName: componentConnection.workflowNodeName,
            } as WorkflowTestConfigurationConnection)
        );
    });

    const form = useForm<WorkflowTestConfiguration>({
        defaultValues: {
            ...workflowTestConfiguration,
            connections: workflowTestConfigurationConnections,
        },
    });

    const {control, formState, handleSubmit} = form;

    const inputs: WorkflowInput[] = workflow.inputs ?? [];

    const {data: connections} = useGetConnectionsQuery!({});

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: componentConnection?.componentName!,
            componentVersion: componentConnection?.componentVersion!,
        },
        !!componentConnection
    );

    const queryClient = useQueryClient();

    const saveWorkflowTestConfigurationMutation = useSaveWorkflowTestConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations});

            onClose();
        },
    });

    function saveWorkflowTestConfiguration(workflowTestConfiguration: WorkflowTestConfiguration) {
        workflowTestConfiguration = {
            ...workflowTestConfiguration,
            connections: workflowTestConfiguration.connections?.filter((connection) => connection.connectionId),
        };

        saveWorkflowTestConfigurationMutation.mutate({
            workflowId: workflow.id!,
            workflowTestConfiguration,
        });
    }

    const getConnectionsToRender = (): Array<{
        connection: ComponentConnection;
        groupedIndices?: number[];
        index: number;
    }> => {
        if (!groupConnections) {
            return componentConnections.map((connection, index) => ({connection, index}));
        }

        const connectionGroupMap = new Map<string, number[]>();

        for (const [index, connection] of componentConnections.entries()) {
            const componentName = connection.componentName;

            if (!connectionGroupMap.has(componentName)) {
                connectionGroupMap.set(componentName, []);
            }

            connectionGroupMap.get(componentName)!.push(index);
        }

        return Array.from(connectionGroupMap.values()).map((indices) => ({
            connection: componentConnections[indices[0]],
            groupedIndices: indices.length > 1 ? indices : undefined,
            index: indices[0],
        }));
    };

    const connectionsToRender = getConnectionsToRender();

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent
                className="max-w-workflow-test-configuration-dialog-width"
                onInteractOutside={(event) => event.preventDefault()}
            >
                <Form {...form}>
                    <form onSubmit={handleSubmit((values) => saveWorkflowTestConfiguration(values))}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>Workflow Test Configuration</DialogTitle>

                                <DialogDescription>
                                    Set workflow input, trigger output values and test connections.
                                </DialogDescription>
                            </div>

                            <DialogCloseButton />
                        </DialogHeader>

                        <div className="max-h-workflow-test-configuration-dialog-height overflow-y-auto">
                            <div className="space-y-4 py-4">
                                {inputs && inputs.length > 0 && (
                                    <div className="space-y-2">
                                        <Label className="text-content-neutral-secondary">Inputs</Label>

                                        <Properties
                                            control={control}
                                            controlPath="inputs"
                                            formState={formState}
                                            properties={inputs.map((input) => {
                                                if (input.type === 'string') {
                                                    return {
                                                        controlType: 'TEXT',
                                                        type: 'STRING',
                                                        ...input,
                                                    } as PropertyAllType;
                                                } else if (input.type === 'number') {
                                                    return {
                                                        type: 'NUMBER',
                                                        ...input,
                                                    } as PropertyAllType;
                                                } else {
                                                    return {
                                                        controlType: 'SELECT',
                                                        type: 'BOOLEAN',
                                                        ...input,
                                                    } as PropertyAllType;
                                                }
                                            })}
                                        />
                                    </div>
                                )}

                                {componentConnections && componentConnections.length > 0 && (
                                    <div className="space-y-2">
                                        <Label className="text-content-neutral-secondary">Connections</Label>

                                        <div className="space-y-4">
                                            {connectionsToRender.map(
                                                ({connection, groupedIndices, index}) =>
                                                    connections && (
                                                        <WorkflowTestConfigurationFormField
                                                            componentConnection={connection}
                                                            connectionDialogAllowed={connectionDialogAllowed}
                                                            connections={connections}
                                                            form={form}
                                                            groupedIndices={groupedIndices}
                                                            index={index}
                                                            key={`${connection.workflowNodeName}_${connection.key}`}
                                                            setComponentConnection={setComponentConnection}
                                                            setShowNewConnectionDialog={setShowNewConnectionDialog}
                                                        />
                                                    )
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>

                        <DialogFooter className="flex items-center">
                            <div className="mr-auto flex items-center gap-2">
                                {componentConnections.length > 1 && (
                                    <>
                                        <Switch checked={groupConnections} onCheckedChange={setGroupConnections} />

                                        <span className="text-sm font-semibold">Group Connections</span>

                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <InfoIcon className="size-4 cursor-default text-content-onsurface-secondary" />
                                            </TooltipTrigger>

                                            <TooltipContent>Connections grouped by their app.</TooltipContent>
                                        </Tooltip>
                                    </>
                                )}
                            </div>

                            <DialogClose asChild>
                                <Button label="Cancel" type="button" variant="outline" />
                            </DialogClose>

                            <Button label="Save" type="submit" />
                        </DialogFooter>
                    </form>
                </Form>

                {showNewConnectionDialog && componentDefinitions && (
                    <Portal.Root>
                        <ConnectionDialog
                            componentDefinition={componentDefinition}
                            componentDefinitions={componentDefinitions}
                            connectionTagsQueryKey={ConnectionKeys!.connectionTags}
                            connectionsQueryKey={ConnectionKeys!.connections}
                            onClose={() => setShowNewConnectionDialog(false)}
                            useCreateConnectionMutation={useCreateConnectionMutation}
                            useGetConnectionTagsQuery={useGetConnectionTagsQuery!}
                        />
                    </Portal.Root>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowTestConfigurationDialog;
