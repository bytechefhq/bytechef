import Button from '@/components/Button/Button';
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
import {PlusIcon} from 'lucide-react';
import {Dispatch, SetStateAction, useState} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

interface WorkflowTestConfigurationDialogProps {
    onClose: () => void;
    workflow: Workflow;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const WorkflowTestConfigurationFormField = ({
    componentConnection,
    connectionDialogAllowed,
    connections,
    form,
    index,
    setComponentConnection,
    setShowNewConnectionDialog,
}: {
    componentConnection: ComponentConnection;
    connectionDialogAllowed: boolean;
    connections: ConnectionI[];
    form: UseFormReturn<WorkflowTestConfiguration>;
    index: number;
    setShowNewConnectionDialog: Dispatch<SetStateAction<boolean>>;
    setComponentConnection: Dispatch<SetStateAction<ComponentConnection | undefined>>;
}) => {
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

                                <span className="ml-0.5 text-xs text-gray-500">
                                    {`(${componentConnection.workflowNodeName} - ${componentConnection.key})`}
                                </span>
                            </FormLabel>

                            <Select
                                onValueChange={field.onChange}
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
                                        <Label className="text-gray-500">Inputs</Label>

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
                                        <Label className="text-gray-500">Connections</Label>

                                        <div className="space-y-4">
                                            {componentConnections.map(
                                                (workflowConnection, index) =>
                                                    connections && (
                                                        <WorkflowTestConfigurationFormField
                                                            componentConnection={workflowConnection}
                                                            connectionDialogAllowed={connectionDialogAllowed}
                                                            connections={connections}
                                                            form={form}
                                                            index={index}
                                                            key={`${workflowConnection.workflowNodeName}_${
                                                                workflowConnection.key
                                                            }`}
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

                        <DialogFooter>
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
