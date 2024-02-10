import {Switch} from '@/components/ui/switch';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ProjectInstanceModel, WorkflowConnectionModel, WorkflowModel} from '@/middleware/automation/configuration';
import ProjectInstanceDialogWorkflowsStepItemConfiguration from '@/pages/automation/project-instances/components/ProjectInstanceDialogWorkflowsStepItemConfiguration';
import ProjectInstanceDialogWorkflowsStepItemConnection from '@/pages/automation/project-instances/components/ProjectInstanceDialogWorkflowsStepItemConnection';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-instances/stores/useWorkflowsEnabledStore';
import {Control, UseFormRegister, UseFormSetValue} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

export interface ProjectInstanceDialogWorkflowListItemProps {
    control: Control<ProjectInstanceModel>;
    formState: FormState<ProjectInstanceModel>;
    label: string;
    register: UseFormRegister<ProjectInstanceModel>;
    setValue: UseFormSetValue<ProjectInstanceModel>;
    switchHidden?: boolean;
    workflow: WorkflowModel;
    workflowIndex: number;
}

const ProjectInstanceDialogWorkflowsStepItem = ({
    control,
    formState,
    label,
    register,
    setValue,
    switchHidden = false,
    workflow,
    workflowIndex,
}: ProjectInstanceDialogWorkflowListItemProps) => {
    const [setWorkflowEnabled, workflowEnabled] = useWorkflowsEnabledStore(
        useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [setWorkflowEnabled, workflowEnabledMap])
    );

    let workflowConnections: WorkflowConnectionModel[] = [];

    workflow.tasks?.forEach((task) => {
        if (task.connections) {
            workflowConnections = workflowConnections.concat(task.connections);
        }
    });

    workflow.triggers?.forEach((trigger) => {
        if (trigger.connections) {
            workflowConnections = workflowConnections.concat(trigger.connections);
        }
    });

    return (
        <div>
            {register && (
                <input
                    type="hidden"
                    {...register(`projectInstanceWorkflows.${workflowIndex!}.workflowId`, {value: workflow.id})}
                />
            )}

            {!switchHidden && (
                <div className="flex cursor-pointer justify-between py-2">
                    <span className="font-semibold">{label}</span>

                    <Switch
                        checked={workflowEnabled.get(workflow.id!)}
                        className={twMerge(
                            'cursor-pointer rounded-full border-2 border-transparent bg-gray-200 transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2',
                            workflowEnabled.get(workflow.id!) && 'bg-blue-600'
                        )}
                        onClick={() => {
                            setValue(
                                `projectInstanceWorkflows.${workflowIndex!}.enabled`,
                                !workflowEnabled.get(workflow.id!)
                            );
                            setWorkflowEnabled(workflow.id!, !workflowEnabled.get(workflow.id!));
                        }}
                    >
                        <span
                            aria-hidden="true"
                            className={twMerge(
                                'pointer-events-none inline-block size-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                                workflowEnabled.get(workflow.id!) ? 'translate-x-5' : 'translate-x-0'
                            )}
                        />
                    </Switch>
                </div>
            )}

            {(workflowEnabled.get(workflow.id!) || switchHidden) && (
                <div className="mt-2">
                    <Tabs aria-label="Tabs" defaultValue="configuration">
                        <TabsList className="grid w-full grid-cols-2">
                            <TabsTrigger value="configuration">Configuration</TabsTrigger>

                            <TabsTrigger value="connections">Connections</TabsTrigger>
                        </TabsList>

                        <TabsContent value="configuration">
                            <ProjectInstanceDialogWorkflowsStepItemConfiguration
                                formState={formState}
                                register={register}
                                workflow={workflow}
                                workflowIndex={workflowIndex}
                            />
                        </TabsContent>

                        <TabsContent className="grid gap-4" value="connections">
                            {workflowConnections.length ? (
                                workflowConnections.map((workflowConnection, workflowConnectionIndex) => (
                                    <ProjectInstanceDialogWorkflowsStepItemConnection
                                        control={control}
                                        key={workflowConnectionIndex + '_' + workflowConnection.key}
                                        workflowConnection={workflowConnection}
                                        workflowConnectionIndex={workflowConnectionIndex}
                                        workflowConnectionsCount={workflowConnections.length}
                                        workflowIndex={workflowIndex}
                                    />
                                ))
                            ) : (
                                <p className="text-sm">No defined connections.</p>
                            )}
                        </TabsContent>
                    </Tabs>
                </div>
            )}
        </div>
    );
};

export default ProjectInstanceDialogWorkflowsStepItem;
