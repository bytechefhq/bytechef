import ComboBox from '@/components/ComboBox';
import EmptyList from '@/components/EmptyList';
import {Button} from '@/components/ui/button';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    ComponentDefinitionBasicModel,
    WorkflowConnectionModel,
    WorkflowModel,
    WorkflowTestConfigurationConnectionModel,
} from '@/middleware/platform/configuration';
import {useCreateConnectionMutation, useUpdateConnectionMutation} from '@/mutations/automation/connections.mutations';
import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/mutations/platform/workflowTestConfigurations.mutations';
import {useConnectionNoteStore} from '@/pages/automation/project/stores/useConnectionNoteStore';
import ConnectionDialog from '@/pages/platform/connection/components/ConnectionDialog';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/queries/automation/connections.queries';
import {WorkflowKeys} from '@/queries/automation/workflows.queries';
import {
    useGetComponentDefinitionQuery,
    useGetComponentDefinitionsQuery,
} from '@/queries/platform/componentDefinitions.queries';
import {
    WorkflowTestConfigurationKeys,
    useGetWorkflowTestConfigurationConnectionsQuery,
} from '@/queries/platform/workflowTestConfigurations.queries';
import {WorkflowDefinitionType, WorkflowTaskType} from '@/types/types';
import {zodResolver} from '@hookform/resolvers/zod';
import {Cross2Icon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {useQueryClient} from '@tanstack/react-query';
import {LinkIcon, PlusIcon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const SPACE = 4;

const connectionFormSchema = z.object({
    componentName: z.string(),
    componentVersion: z.number(),
    name: z.string().min(2, {
        message: 'Name must be at least 3 characters.',
    }),
});

const ConnectionLabel = ({
    onRemoveClick,
    workflowConnection,
    workflowConnectionsCount,
}: {
    onRemoveClick: () => void;
    workflowConnection: WorkflowConnectionModel;
    workflowConnectionsCount: number;
}) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });

    return (
        <div className="flex items-center justify-between">
            <div className="space-x-1">
                {componentDefinition && (
                    <Label>
                        {`${componentDefinition?.title}`}

                        {workflowConnection.required && <span className="ml-0.5 leading-3 text-red-500">*</span>}
                    </Label>
                )}

                {workflowConnectionsCount > 1 && (
                    <Tooltip>
                        <TooltipTrigger>
                            <Label className="text-sm text-muted-foreground">{workflowConnection.key}</Label>
                        </TooltipTrigger>

                        <TooltipContent>Workflow Connection Key</TooltipContent>
                    </Tooltip>
                )}
            </div>

            <div>
                <Button className="text-destructive" onClick={onRemoveClick} size="sm" variant="link">
                    Remove
                </Button>
            </div>
        </div>
    );
};

const ConnectionPopover = ({onSubmit}: {onSubmit: (values: z.infer<typeof connectionFormSchema>) => void}) => {
    const [open, setOpen] = useState(false);

    const form = useForm<z.infer<typeof connectionFormSchema>>({
        defaultValues: {
            componentName: '',
            name: '',
        },
        resolver: zodResolver(connectionFormSchema),
    });

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                <Button size="sm" variant="secondary">
                    Add Connection
                </Button>
            </PopoverTrigger>

            <PopoverContent align="end" className="min-w-[400px]">
                {form.formState.errors.componentVersion?.message}

                <header className="flex items-center justify-between">
                    <span className="font-medium">Add Connection</span>

                    <PopoverClose asChild onClick={() => form.reset()}>
                        <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                    </PopoverClose>
                </header>

                <Form {...form}>
                    <form
                        onSubmit={form.handleSubmit((values) => {
                            onSubmit(values);
                            setOpen(false);
                            form.reset();
                        })}
                    >
                        <main className="my-2 space-y-4">
                            <FormField
                                control={form.control}
                                name="name"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Name</FormLabel>

                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                rules={{required: true}}
                            />

                            <FormField
                                control={form.control}
                                name="componentName"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Component</FormLabel>

                                        <FormControl>
                                            {componentDefinitions && (
                                                <ComboBox
                                                    items={componentDefinitions.map((componentDefinition) => ({
                                                        componentDefinition,
                                                        icon: componentDefinition.icon,
                                                        label: componentDefinition.title!,
                                                        value: componentDefinition.name,
                                                    }))}
                                                    maxHeight={true}
                                                    name="componentName"
                                                    onBlur={field.onBlur}
                                                    onChange={(item) => {
                                                        const componentDefinition =
                                                            item?.componentDefinition as ComponentDefinitionBasicModel;

                                                        form.setValue('componentName', componentDefinition.name, {
                                                            shouldDirty: true,
                                                        });
                                                        form.setValue('componentVersion', componentDefinition.version, {
                                                            shouldDirty: true,
                                                        });
                                                    }}
                                                    value={field.value}
                                                />
                                            )}
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                rules={{required: true}}
                            />
                        </main>

                        <footer className="flex items-center justify-end space-x-2">
                            <Button type="submit">Add</Button>
                        </footer>
                    </form>
                </Form>
            </PopoverContent>
        </Popover>
    );
};

const ConnectionSelect = ({
    workflowConnection,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnection,
}: {
    workflowConnection: WorkflowConnectionModel;
    workflowId: string;
    workflowNodeName: string;
    workflowTestConfigurationConnection?: WorkflowTestConfigurationConnectionModel;
}) => {
    const [showEditConnectionDialog, setShowEditConnectionDialog] = useState(false);

    let connectionId: number | undefined;

    if (workflowTestConfigurationConnection) {
        connectionId = workflowTestConfigurationConnection.connectionId;
    }

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: connections} = useGetConnectionsQuery(
        {
            componentName: componentDefinition?.name!,
            connectionVersion: componentDefinition?.connection?.version,
        },
        !!componentDefinition
    );

    const queryClient = useQueryClient();

    const saveWorkflowTestConfigurationConnectionMutation = useSaveWorkflowTestConfigurationConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
            });
        },
    });

    const handleValueChange = (connectionId: number, workflowConnectionKey: string) => {
        saveWorkflowTestConfigurationConnectionMutation.mutate({
            saveWorkflowTestConfigurationConnectionRequestModel: {
                connectionId,
            },
            workflowConnectionKey,
            workflowId,
            workflowNodeName,
        });
    };

    return (
        <>
            <Select
                onValueChange={(value) => handleValueChange(+value, workflowConnection.key)}
                required={workflowConnection.required}
                value={connectionId ? connectionId.toString() : undefined}
            >
                <div className="flex space-x-2">
                    <SelectTrigger>
                        <SelectValue placeholder="Choose Connection..." />
                    </SelectTrigger>

                    <Button
                        className="mt-auto p-2"
                        onClick={() => setShowEditConnectionDialog(true)}
                        title="Create a new connection"
                        variant="outline"
                    >
                        <PlusIcon className="size-5" />
                    </Button>
                </div>

                <SelectContent>
                    {connections &&
                        connections.map((connection) => (
                            <SelectItem key={connection.id} value={connection.id!.toString()}>
                                <div className="flex items-center">
                                    <span className="mr-1 ">{connection.name}</span>

                                    <span className="text-xs text-gray-500">
                                        {connection?.tags?.map((tag) => tag.name).join(', ')}
                                    </span>
                                </div>
                            </SelectItem>
                        ))}
                </SelectContent>
            </Select>

            {showEditConnectionDialog && (
                <ConnectionDialog
                    componentDefinition={componentDefinition}
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setShowEditConnectionDialog(false)}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                    useUpdateConnectionMutation={useUpdateConnectionMutation}
                />
            )}
        </>
    );
};

const PropertyCodeEditorSheetConnectionsSidebar = ({
    workflow,
    workflowConnections,
    workflowNodeName,
}: {
    workflowConnections: WorkflowConnectionModel[];
    workflow: WorkflowModel;
    workflowNodeName: string;
}) => {
    const [showEditConnectionDialog, setShowEditConnectionDialog] = useState(false);

    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore();

    const {data: workflowTestConfigurationConnections} = useGetWorkflowTestConfigurationConnectionsQuery({
        workflowId: workflow.id!,
        workflowNodeName,
    });

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
        onSuccess: (workflow: WorkflowModel) => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const handleOnSubmit = (values: z.infer<typeof connectionFormSchema>) => {
        let workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition!);

        const scriptWorkflowTask = workflowDefinition.tasks?.filter((task) => task.name === workflowNodeName)[0]!;

        workflowDefinition = {
            ...workflowDefinition,
            tasks: [
                ...workflowDefinition.tasks!.map((task) => {
                    if (task.name === workflowNodeName) {
                        return {
                            ...scriptWorkflowTask,
                            connections: {
                                ...(scriptWorkflowTask.connections ?? {}),
                                [values.name]: {
                                    componentName: values.componentName,
                                    componentVersion: values.componentVersion,
                                },
                            },
                        } as WorkflowTaskType;
                    } else {
                        return task;
                    }
                }),
            ],
        };

        updateWorkflowMutation.mutate({
            id: workflow.id!,
            workflowModel: {
                definition: JSON.stringify(workflowDefinition, null, SPACE),
                version: workflow.version,
            },
        });
    };

    const handleOnRemoveClick = (workflowConnectionKey: string) => {
        let workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition!);

        const scriptWorkflowTask = workflowDefinition.tasks?.filter((task) => task.name === workflowNodeName)[0]!;

        delete scriptWorkflowTask.connections[workflowConnectionKey];

        workflowDefinition = {
            ...workflowDefinition,
            tasks: [
                ...workflowDefinition.tasks!.map((task) => {
                    if (task.name === workflowNodeName) {
                        return {
                            ...scriptWorkflowTask,
                            connections: {
                                ...(scriptWorkflowTask.connections ?? {}),
                            },
                        };
                    } else {
                        return task;
                    }
                }),
            ],
        };

        updateWorkflowMutation.mutate({
            id: workflow.id!,
            workflowModel: {
                definition: JSON.stringify(workflowDefinition, null, SPACE),
                version: workflow.version,
            },
        });
    };

    return (
        <div className="flex h-full flex-col gap-4 overflow-auto p-4 pt-3.5">
            <div className="flex  items-center font-semibold">
                <span>Connections</span>
            </div>

            {workflowConnections?.length ? (
                <>
                    {workflowConnections.map((workflowConnection) => (
                        <fieldset className="space-y-2" key={workflowConnection.key}>
                            <ConnectionLabel
                                onRemoveClick={() => handleOnRemoveClick(workflowConnection.key)}
                                workflowConnection={workflowConnection}
                                workflowConnectionsCount={workflowConnections.length}
                            />

                            <ConnectionSelect
                                workflowConnection={workflowConnection}
                                workflowId={workflow.id!}
                                workflowNodeName={workflowNodeName}
                                workflowTestConfigurationConnection={
                                    workflowTestConfigurationConnections &&
                                    workflowTestConfigurationConnections.length > 0 &&
                                    workflowTestConfigurationConnections
                                        ? workflowTestConfigurationConnections.filter(
                                              (workflowTestConfigurationConnection) =>
                                                  workflowTestConfigurationConnection.workflowConnectionKey ===
                                                  workflowConnection.key
                                          )[0]
                                        : undefined
                                }
                            />
                        </fieldset>
                    ))}

                    <div className="flex justify-end">
                        <ConnectionPopover onSubmit={handleOnSubmit} />
                    </div>
                </>
            ) : (
                <div className="flex flex-1 flex-col items-center justify-center">
                    <EmptyList
                        button={
                            <Button onClick={() => setShowEditConnectionDialog(true)} title="Create a new connection">
                                Create a connection
                            </Button>
                        }
                        icon={<LinkIcon className="size-6 text-gray-400" />}
                        message="You have not created any connections for this component yet."
                        title="No Connections"
                    />

                    {showConnectionNote && (
                        <div className="mt-4 flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
                            <div className="flex items-center pb-2">
                                <span className="font-medium">Note</span>

                                <button
                                    className="ml-auto p-0"
                                    onClick={() => setShowConnectionNote(false)}
                                    title="Close the note"
                                >
                                    <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                                </button>
                            </div>

                            <p className="text-sm text-gray-800">
                                The selected connections are used for testing purposes only.
                            </p>
                        </div>
                    )}
                </div>
            )}

            {showEditConnectionDialog && (
                <ConnectionDialog
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setShowEditConnectionDialog(false)}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                    useUpdateConnectionMutation={useUpdateConnectionMutation}
                />
            )}
        </div>
    );
};

export default PropertyCodeEditorSheetConnectionsSidebar;
