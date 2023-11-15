import Properties from '@/components/Properties/Properties';
import {Button} from '@/components/ui/button';
import {
    FormControl,
    FormDescription,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from '@/components/ui/form';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {Switch} from '@/components/ui/switch';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {
    ProjectInstanceModel,
    WorkflowConnectionModel,
    WorkflowModel,
} from '@/middleware/helios/configuration';
import ConnectionDialog from '@/pages/automation/connections/components/ConnectionDialog';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-instances/stores/useWorkflowsEnabledStore';
import {useGetComponentDefinitionQuery} from '@/queries/componentDefinitions.queries';
import {useGetConnectionsQuery} from '@/queries/connections.queries';
import {PropertyType} from '@/types/projectTypes';
import * as Portal from '@radix-ui/react-portal';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';
import {Control, UseFormRegister} from 'react-hook-form';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface ConfigurationProps {
    formState: FormState<FieldValues>;
    register: UseFormRegister<ProjectInstanceModel>;
    workflow: WorkflowModel;
    workflowIndex: number;
}

const Configuration = ({
    formState,
    register,
    workflow,
    workflowIndex,
}: ConfigurationProps) => {
    return workflow.inputs?.length ? (
        <Properties
            formState={formState}
            path={`projectInstanceWorkflows.${workflowIndex!}.inputs`}
            properties={workflow.inputs.map((input) => {
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
    ) : (
        <p className="text-sm">No defined configuration inputs.</p>
    );
};

interface ConnectionProps {
    control: Control<ProjectInstanceModel>;
    workflowConnection: WorkflowConnectionModel;
    workflowConnectionIndex: number;
    workflowIndex: number;
}

const Connection = ({
    control,
    workflowConnection,
    workflowConnectionIndex,
    workflowIndex,
}: ConnectionProps) => {
    const [showEditConnectionDialog, setShowEditConnectionDialog] =
        useState(false);

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });
    const {data: connections} = useGetConnectionsQuery(
        {
            componentName: workflowConnection.componentName,
            connectionVersion: componentDefinition?.connection?.version,
        },
        !!componentDefinition
    );

    return (
        <>
            <FormField
                control={control}
                name={`projectInstanceWorkflows.${workflowIndex!}.connections.${workflowConnectionIndex}.connectionId`}
                render={({field}) => (
                    <FormItem>
                        <FormLabel>
                            {`${componentDefinition?.title} `}

                            <span className="text-xs text-gray-500">
                                ({workflowConnection.key})
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
                                        onClick={() =>
                                            setShowEditConnectionDialog(true)
                                        }
                                        title="Create a new connection"
                                        variant="outline"
                                    >
                                        <PlusIcon className="h-5 w-5" />
                                    </Button>
                                </div>
                            </FormControl>

                            <SelectContent>
                                {connections &&
                                    connections.map((connection) => (
                                        <SelectItem
                                            key={connection.id}
                                            value={connection.id!.toString()}
                                        >
                                            <span className="flex items-center">
                                                <span className="mr-1 ">
                                                    {connection.name}
                                                </span>

                                                <span className="text-xs text-gray-500">
                                                    {connection?.tags
                                                        ?.map((tag) => tag.name)
                                                        .join(', ')}
                                                </span>
                                            </span>
                                        </SelectItem>
                                    ))}
                            </SelectContent>
                        </Select>

                        <FormDescription>
                            {`Choose connection for the ${componentDefinition?.title}`}

                            <span className="text-xs text-gray-500">
                                ({workflowConnection.key})
                            </span>

                            {` component.`}
                        </FormDescription>

                        <FormMessage />
                    </FormItem>
                )}
                rules={{required: true}}
            />

            <FormField
                control={control}
                defaultValue={workflowConnection.key}
                name={`projectInstanceWorkflows.${workflowIndex!}.connections.${workflowConnectionIndex}.key`}
                render={({field}) => <input type="hidden" {...field} />}
            />

            <FormField
                control={control}
                defaultValue={workflowConnection.operationName}
                name={`projectInstanceWorkflows.${workflowIndex!}.connections.${workflowConnectionIndex}.operationName`}
                render={({field}) => <input type="hidden" {...field} />}
            />

            {showEditConnectionDialog && (
                <Portal.Root>
                    <ConnectionDialog
                        componentDefinition={componentDefinition}
                        onClose={() => setShowEditConnectionDialog(false)}
                    />
                </Portal.Root>
            )}
        </>
    );
};

export interface ProjectInstanceDialogWorkflowListItemProps {
    control: Control<ProjectInstanceModel>;
    formState: FormState<ProjectInstanceModel>;
    label: string;
    register: UseFormRegister<ProjectInstanceModel>;
    switchHidden?: boolean;
    workflow: WorkflowModel;
    workflowIndex: number;
}

const ProjectInstanceDialogWorkflowListItem = ({
    control,
    formState,
    label,
    register,
    switchHidden = false,
    workflow,
    workflowIndex,
}: ProjectInstanceDialogWorkflowListItemProps) => {
    const [setWorkflowEnabled, workflowEnabledMap] = useWorkflowsEnabledStore(
        useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [
            setWorkflowEnabled,
            workflowEnabledMap,
        ])
    );

    let workflowConnections: WorkflowConnectionModel[] = [];

    workflow.tasks?.forEach((task) => {
        if (task.connections) {
            workflowConnections = workflowConnections.concat(task.connections);
        }
    });

    workflow.triggers?.forEach((trigger) => {
        if (trigger.connections) {
            workflowConnections = workflowConnections.concat(
                trigger.connections
            );
        }
    });

    return (
        <div>
            {register && (
                <input
                    type="hidden"
                    {...register(
                        `projectInstanceWorkflows.${workflowIndex!}.workflowId`,
                        {value: workflow.id}
                    )}
                />
            )}

            {!switchHidden && (
                <div className="flex cursor-pointer justify-between py-2">
                    <span className="font-semibold">{label}</span>

                    <Switch
                        checked={workflowEnabledMap.get(workflow.id!)}
                        className={twMerge(
                            'cursor-pointer rounded-full border-2 border-transparent bg-gray-200 transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2',
                            workflowEnabledMap.get(workflow.id!) &&
                                'bg-blue-600'
                        )}
                        onClick={() => {
                            setWorkflowEnabled(
                                workflow.id!,
                                !workflowEnabledMap.get(workflow.id!)
                            );
                        }}
                    >
                        <span
                            aria-hidden="true"
                            className={twMerge(
                                'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                                workflowEnabledMap.get(workflow.id!)
                                    ? 'translate-x-5'
                                    : 'translate-x-0'
                            )}
                        />
                    </Switch>
                </div>
            )}

            {workflowEnabledMap.get(workflow.id!) && register && (
                <input
                    type="hidden"
                    {...register(
                        `projectInstanceWorkflows.${workflowIndex!}.enabled`,
                        {value: workflowEnabledMap.get(workflow.id!)}
                    )}
                />
            )}

            {(workflowEnabledMap.get(workflow.id!) || switchHidden) && (
                <div className="mt-2">
                    <Tabs aria-label="Tabs" defaultValue="configuration">
                        <TabsList className="grid w-full grid-cols-2">
                            <TabsTrigger value="configuration">
                                Configuration
                            </TabsTrigger>

                            <TabsTrigger value="connections">
                                Connections
                            </TabsTrigger>
                        </TabsList>

                        <TabsContent value="configuration">
                            <Configuration
                                formState={formState}
                                register={register}
                                workflow={workflow}
                                workflowIndex={workflowIndex}
                            />
                        </TabsContent>

                        <TabsContent value="connections">
                            {workflowConnections.length ? (
                                workflowConnections.map(
                                    (
                                        workflowConnection,
                                        workflowConnectionConnectionIndex
                                    ) => (
                                        <Connection
                                            control={control}
                                            key={workflowConnection.key}
                                            workflowConnection={
                                                workflowConnection
                                            }
                                            workflowConnectionIndex={
                                                workflowConnectionConnectionIndex
                                            }
                                            workflowIndex={workflowIndex}
                                        />
                                    )
                                )
                            ) : (
                                <p>No defined connections.</p>
                            )}
                        </TabsContent>
                    </Tabs>
                </div>
            )}
        </div>
    );
};

export default ProjectInstanceDialogWorkflowListItem;
