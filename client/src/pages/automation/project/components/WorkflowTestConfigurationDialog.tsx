import Properties from '@/components/Properties/Properties';
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
import {WorkflowConnectionModel, WorkflowInputModel, WorkflowModel} from '@/middleware/hermes/configuration';
import {WorkflowTestConfigurationModel} from '@/middleware/hermes/test';
import {
    useCreateWorkflowTestConfigurationMutation,
    useUpdateWorkflowTestConfigurationMutation,
} from '@/mutations/workflowTestConfigurations.mutations';
import ConnectionDialog from '@/pages/automation/connections/components/ConnectionDialog';
import {useGetComponentDefinitionQuery} from '@/queries/componentDefinitions.queries';
import {useGetConnectionsQuery} from '@/queries/connections.queries';
import {WorkflowTestConfigurationKeys} from '@/queries/workflowTestConfigurations.queries';
import {PropertyType} from '@/types/projectTypes';
import {Cross2Icon} from '@radix-ui/react-icons';
import * as Portal from '@radix-ui/react-portal';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';

interface WorkflowTestConfigurationDialogProps {
    onClose: () => void;
    workflow: WorkflowModel;
    workflowTestConfiguration?: WorkflowTestConfigurationModel;
}

const WorkflowTestConfigurationDialog = ({
    onClose,
    workflow,
    workflowTestConfiguration,
}: WorkflowTestConfigurationDialogProps) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);
    const [workflowConnection, setWorkflowConnection] = useState<WorkflowConnectionModel | undefined>();

    const form = useForm<WorkflowTestConfigurationModel>({
        defaultValues: workflowTestConfiguration,
    });

    const {formState, handleSubmit, register} = form;

    const inputs: WorkflowInputModel[] = workflow.inputs ?? [];

    const workflowConnections: WorkflowConnectionModel[] = (workflow?.tasks ?? [])
        .flatMap((task) => (task.connections ? task.connections : []))
        .filter((workflowConnection) => !workflowConnection.id);

    const {data: connections} = useGetConnectionsQuery({});

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: workflowConnection?.componentName!,
            componentVersion: workflowConnection?.componentVersion!,
        },
        !!workflowConnection
    );

    const queryClient = useQueryClient();

    const createWorkflowTestConfigurationMutation = useCreateWorkflowTestConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations});

            onClose();
        },
    });
    const updateWorkflowTestConfigurationMutation = useUpdateWorkflowTestConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations});

            onClose();
        },
    });

    function saveWorkflowTestConfiguration(workflowTestConfigurationModel: WorkflowTestConfigurationModel) {
        if (workflowTestConfigurationModel.id) {
            updateWorkflowTestConfigurationMutation.mutate({
                id: workflowTestConfigurationModel.id,
                workflowTestConfigurationModel,
            });
        } else {
            workflowTestConfigurationModel.workflowId = workflow.id;

            createWorkflowTestConfigurationMutation.mutate({workflowTestConfigurationModel});
        }
    }

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent className="max-w-[600px]" onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <form onSubmit={handleSubmit((values) => saveWorkflowTestConfiguration(values))}>
                        <DialogHeader>
                            <div className="flex items-center justify-between">
                                <DialogTitle>Workflow Test Configuration</DialogTitle>

                                <DialogClose asChild>
                                    <Cross2Icon className="h-4 w-4 cursor-pointer opacity-70" />
                                </DialogClose>
                            </div>

                            <DialogDescription>
                                Set workflow input, trigger output values and test connections. Click save when you are
                                done.
                            </DialogDescription>
                        </DialogHeader>

                        <div className="max-h-[700px] overflow-y-auto">
                            <div className="space-y-4 py-4">
                                {inputs && inputs.length > 0 && (
                                    <div className="space-y-2">
                                        <Label className="text-gray-500">Inputs</Label>

                                        <Properties
                                            formState={formState}
                                            path="inputs"
                                            properties={inputs.map((input) => {
                                                if (input.type === 'string') {
                                                    return {
                                                        controlType: 'TEXT',
                                                        type: 'STRING',
                                                        ...input,
                                                    } as PropertyType;
                                                } else if (input.type === 'number') {
                                                    return {
                                                        type: 'NUMBER',
                                                        ...input,
                                                    } as PropertyType;
                                                } else {
                                                    return {
                                                        controlType: 'SELECT',
                                                        type: 'BOOLEAN',
                                                        ...input,
                                                    } as PropertyType;
                                                }
                                            })}
                                            register={register}
                                        />
                                    </div>
                                )}

                                {workflowConnections && (
                                    <div className="space-y-2">
                                        <Label className="text-gray-500">Connections</Label>

                                        {workflowConnections.map((workflowConnection, index) => {
                                            const operationConnectionCount = workflowConnections.reduce(
                                                (count, curWorkflowConnection) => {
                                                    if (
                                                        curWorkflowConnection.operationName ===
                                                        workflowConnection.operationName
                                                    ) {
                                                        return count + 1;
                                                    }

                                                    return count;
                                                },
                                                0
                                            );

                                            return (
                                                <div key={index}>
                                                    <FormField
                                                        control={form.control}
                                                        name={`connections.${index}.connectionId`}
                                                        render={({field}) => (
                                                            <FormItem>
                                                                <FormLabel>
                                                                    {'Connection '}

                                                                    <span className="text-xs text-gray-500">
                                                                        {`(${workflowConnection.operationName}${
                                                                            operationConnectionCount > 1
                                                                                ? ' - ' + workflowConnection.key
                                                                                : ''
                                                                        })`}
                                                                    </span>
                                                                </FormLabel>

                                                                <Select
                                                                    defaultValue={
                                                                        field.value ? field.value.toString() : undefined
                                                                    }
                                                                    onValueChange={field.onChange}
                                                                >
                                                                    <FormControl>
                                                                        <div className="flex space-x-2">
                                                                            <SelectTrigger>
                                                                                <SelectValue placeholder="Choose Connection..." />
                                                                            </SelectTrigger>

                                                                            <Button
                                                                                className="mt-auto p-2"
                                                                                onClick={() => {
                                                                                    setWorkflowConnection(
                                                                                        workflowConnection
                                                                                    );
                                                                                    setShowNewConnectionDialog(true);
                                                                                }}
                                                                                title="Create a new connection"
                                                                                type="button"
                                                                                variant="outline"
                                                                            >
                                                                                <PlusIcon className="h-5 w-5" />
                                                                            </Button>
                                                                        </div>
                                                                    </FormControl>

                                                                    <SelectContent>
                                                                        {connections &&
                                                                            connections
                                                                                .filter(
                                                                                    (connection) =>
                                                                                        connection.componentName ===
                                                                                        workflowConnection.componentName
                                                                                )
                                                                                .map((connection) => (
                                                                                    <SelectItem
                                                                                        key={connection.id}
                                                                                        value={connection.id!.toString()}
                                                                                    >
                                                                                        <div className="flex items-center">
                                                                                            <span className="mr-1 ">
                                                                                                {connection.name}
                                                                                            </span>

                                                                                            <span className="text-xs text-gray-500">
                                                                                                {connection?.tags
                                                                                                    ?.map(
                                                                                                        (tag) =>
                                                                                                            tag.name
                                                                                                    )
                                                                                                    .join(', ')}
                                                                                            </span>
                                                                                        </div>
                                                                                    </SelectItem>
                                                                                ))}
                                                                    </SelectContent>
                                                                </Select>

                                                                <FormMessage />
                                                            </FormItem>
                                                        )}
                                                        rules={{
                                                            required: workflowConnection.required,
                                                        }}
                                                    />

                                                    <FormField
                                                        control={form.control}
                                                        defaultValue={workflowConnection.key}
                                                        name={`connections.${index}.key`}
                                                        render={({field}) => <input type="hidden" {...field} />}
                                                    />

                                                    <FormField
                                                        control={form.control}
                                                        defaultValue={workflowConnection.operationName}
                                                        name={`connections.${index}.operationName`}
                                                        render={({field}) => <input type="hidden" {...field} />}
                                                    />
                                                </div>
                                            );
                                        })}
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
                            onClose={() => setShowNewConnectionDialog(false)}
                        />
                    </Portal.Root>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowTestConfigurationDialog;
