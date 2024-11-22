import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import ConnectionDialog from '@/pages/platform/connection/components/ConnectionDialog';
import {ConnectionI, useConnectionQuery} from '@/pages/platform/connection/providers/connectionReactQueryProvider';
import Properties from '@/pages/platform/workflow-editor/components/Properties/Properties';
import {
    Workflow,
    WorkflowConnection,
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
    connections,
    form,
    index,
    setShowNewConnectionDialog,
    setWorkflowConnection,
    workflowConnection,
}: {
    connections: ConnectionI[];
    form: UseFormReturn<WorkflowTestConfiguration>;
    index: number;
    workflowConnection: WorkflowConnection;
    setShowNewConnectionDialog: Dispatch<SetStateAction<boolean>>;
    setWorkflowConnection: Dispatch<SetStateAction<WorkflowConnection | undefined>>;
}) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
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
                                    {`(${workflowConnection.workflowNodeName})`}
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

                                        <Button
                                            className="mt-auto p-2"
                                            onClick={() => {
                                                setWorkflowConnection(workflowConnection);
                                                setShowNewConnectionDialog(true);
                                            }}
                                            title="Create a new connection"
                                            type="button"
                                            variant="outline"
                                        >
                                            <PlusIcon className="size-5" />
                                        </Button>
                                    </div>
                                </FormControl>

                                <SelectContent>
                                    {connections &&
                                        connections
                                            .filter(
                                                (connection) =>
                                                    connection.componentName === workflowConnection.componentName
                                            )
                                            .map((connection) => (
                                                <SelectItem key={connection.id} value={connection.id!.toString()}>
                                                    <div className="flex items-center">
                                                        <span className="mr-1">{connection.name}</span>

                                                        <span className="text-xs text-gray-500">
                                                            {connection?.tags?.map((tag) => tag.name).join(', ')}
                                                        </span>

                                                        <Badge variant="outline">{connection.environment}</Badge>
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
                    required: workflowConnection.required,
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
    const [workflowConnection, setWorkflowConnection] = useState<WorkflowConnection | undefined>();

    const {ConnectionKeys, useCreateConnectionMutation, useGetConnectionTagsQuery, useGetConnectionsQuery} =
        useConnectionQuery();

    const workflowConnections: WorkflowConnection[] = [
        ...(workflow?.triggers ?? []),
        ...(workflow?.tasks ?? []),
    ].flatMap((operation) => (operation.connections ? operation.connections : []));

    const workflowTestConfigurationConnections = workflowConnections.map((workflowConnection) => {
        const workflowTestConfigurationConnection = (workflowTestConfiguration?.connections ?? []).find(
            (curWorkflowTestConfigurationConfiguration) =>
                curWorkflowTestConfigurationConfiguration.workflowNodeName === workflowConnection.workflowNodeName &&
                curWorkflowTestConfigurationConfiguration.workflowConnectionKey === workflowConnection.key
        );

        return (
            workflowTestConfigurationConnection ??
            ({
                workflowConnectionKey: workflowConnection.key,
                workflowNodeName: workflowConnection.workflowNodeName,
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
            componentName: workflowConnection?.componentName!,
            componentVersion: workflowConnection?.componentVersion!,
        },
        !!workflowConnection
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
                        <DialogHeader>
                            <DialogTitle>Workflow Test Configuration</DialogTitle>

                            <DialogDescription>
                                Set workflow input, trigger output values and test connections. Click save when you are
                                done.
                            </DialogDescription>
                        </DialogHeader>

                        <div className="max-h-workflow-test-configuration-dialog-height overflow-y-auto">
                            <div className="space-y-4 py-4">
                                {inputs && inputs.length > 0 && (
                                    <div className="space-y-2">
                                        <Label className="text-gray-500">Inputs</Label>

                                        <Properties
                                            control={control}
                                            formState={formState}
                                            path="inputs"
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

                                {workflowConnections && workflowConnections.length > 0 && (
                                    <div className="space-y-2">
                                        <Label className="text-gray-500">Connections</Label>

                                        <div className="space-y-4">
                                            {workflowConnections.map(
                                                (workflowConnection, index) =>
                                                    connections && (
                                                        <WorkflowTestConfigurationFormField
                                                            connections={connections}
                                                            form={form}
                                                            index={index}
                                                            key={
                                                                workflowConnection.workflowNodeName +
                                                                '_' +
                                                                workflowConnection.key
                                                            }
                                                            setShowNewConnectionDialog={setShowNewConnectionDialog}
                                                            setWorkflowConnection={setWorkflowConnection}
                                                            workflowConnection={workflowConnection}
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
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button type="submit">Save</Button>
                        </DialogFooter>
                    </form>
                </Form>

                {showNewConnectionDialog && (
                    <Portal.Root>
                        <ConnectionDialog
                            componentDefinition={componentDefinition}
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
