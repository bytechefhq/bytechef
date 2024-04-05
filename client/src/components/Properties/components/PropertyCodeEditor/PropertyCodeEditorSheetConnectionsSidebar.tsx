import ComboBox from '@/components/ComboBox';
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
import {useCreateConnectionMutation} from '@/mutations/automation/connections.mutations';
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
import {ReactNode, useState} from 'react';
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
}: {
    onRemoveClick: () => void;
    workflowConnection: WorkflowConnectionModel;
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
                        <span>{componentDefinition?.title}</span>

                        {workflowConnection.required && <span className="ml-0.5 leading-3 text-red-500">*</span>}
                    </Label>
                )}

                <Tooltip>
                    <TooltipTrigger>
                        <Label className="text-sm text-muted-foreground">{workflowConnection.key}</Label>
                    </TooltipTrigger>

                    <TooltipContent>Workflow Connection Key</TooltipContent>
                </Tooltip>
            </div>

            <Button className="text-destructive" onClick={onRemoveClick} size="sm" variant="link">
                Remove
            </Button>
        </div>
    );
};

const ComponentPopover = ({
    onSubmit,
    triggerNode,
}: {
    onSubmit: (values: z.infer<typeof connectionFormSchema>) => void;
    triggerNode?: ReactNode;
}) => {
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
                {triggerNode ? (
                    triggerNode
                ) : (
                    <Button size="sm" variant="secondary">
                        Add Component
                    </Button>
                )}
            </PopoverTrigger>

            <PopoverContent align="end" className="min-w-[400px]">
                <header className="flex items-center justify-between">
                    <span className="font-medium">Add Component</span>

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
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);

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
                        onClick={() => setShowNewConnectionDialog(true)}
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

            {showNewConnectionDialog && (
                <ConnectionDialog
                    componentDefinition={componentDefinition}
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setShowNewConnectionDialog(false)}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
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
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);

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
            <div className="flex items-center font-semibold">
                <span>Connections</span>
            </div>

            {workflowConnections?.length ? (
                <>
                    {workflowConnections.map((workflowConnection) => {
                        const workflowTestConfigurationConnection =
                            workflowTestConfigurationConnections &&
                            workflowTestConfigurationConnections.length > 0 &&
                            workflowTestConfigurationConnections
                                ? workflowTestConfigurationConnections.filter(
                                      (workflowTestConfigurationConnection) =>
                                          workflowTestConfigurationConnection.workflowConnectionKey ===
                                          workflowConnection.key
                                  )[0]
                                : undefined;

                        return (
                            <fieldset className="space-y-2" key={workflowConnection.key}>
                                <ConnectionLabel
                                    onRemoveClick={() => handleOnRemoveClick(workflowConnection.key)}
                                    workflowConnection={workflowConnection}
                                />

                                <ConnectionSelect
                                    workflowConnection={workflowConnection}
                                    workflowId={workflow.id!}
                                    workflowNodeName={workflowNodeName}
                                    workflowTestConfigurationConnection={workflowTestConfigurationConnection}
                                />
                            </fieldset>
                        );
                    })}

                    <div className="flex justify-end">
                        <ComponentPopover onSubmit={handleOnSubmit} />
                    </div>
                </>
            ) : (
                <div className="flex flex-1 flex-col items-center">
                    <div className="mt-16 w-full place-self-center px-2 3xl:mx-auto 3xl:w-4/5">
                        <div className="text-center">
                            <span className="mx-auto inline-block">
                                <LinkIcon className="size-6 text-gray-400" />
                            </span>

                            <h3 className="mt-2 text-sm font-semibold">No defined components</h3>

                            <p className="mt-1 text-sm text-gray-500">
                                You have not defined any component and its connection to use inside this script yet.
                            </p>

                            <div className="mt-6">
                                <ComponentPopover
                                    onSubmit={handleOnSubmit}
                                    triggerNode={<Button>Add Component</Button>}
                                />
                            </div>
                        </div>
                    </div>

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

            {showNewConnectionDialog && (
                <ConnectionDialog
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setShowNewConnectionDialog(false)}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                />
            )}
        </div>
    );
};

export default PropertyCodeEditorSheetConnectionsSidebar;
