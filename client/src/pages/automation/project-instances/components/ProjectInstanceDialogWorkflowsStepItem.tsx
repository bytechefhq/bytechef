import {Label} from '@/components/ui/label';
import {Switch} from '@/components/ui/switch';
import {ProjectInstanceModel, WorkflowConnectionModel, WorkflowModel} from '@/middleware/automation/configuration';
import ProjectInstanceDialogWorkflowsStepItemConnections from '@/pages/automation/project-instances/components/ProjectInstanceDialogWorkflowsStepItemConnections';
import ProjectInstanceDialogWorkflowsStepItemInputs from '@/pages/automation/project-instances/components/ProjectInstanceDialogWorkflowsStepItemInputs';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-instances/stores/useWorkflowsEnabledStore';
import {Control, UseFormSetValue} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

export interface ProjectInstanceDialogWorkflowListItemProps {
    control: Control<ProjectInstanceModel>;
    formState: FormState<ProjectInstanceModel>;
    label?: string;
    setValue: UseFormSetValue<ProjectInstanceModel>;
    switchHidden?: boolean;
    workflow: WorkflowModel;
    workflowIndex: number;
}

const ProjectInstanceDialogWorkflowsStepItem = ({
    control,
    formState,
    label,
    setValue,
    switchHidden = false,
    workflow,
    workflowIndex,
}: ProjectInstanceDialogWorkflowListItemProps) => {
    const [setWorkflowEnabled, workflowEnabledMap] = useWorkflowsEnabledStore(
        useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [setWorkflowEnabled, workflowEnabledMap])
    );

    const workflowConnections: WorkflowConnectionModel[] = (workflow?.tasks ?? [])
        .flatMap((task) => task.connections ?? [])
        .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []));

    return (
        <div>
            {!switchHidden && (
                <div className="flex cursor-pointer justify-between py-2">
                    <span className="font-semibold">{label}</span>

                    <Switch
                        checked={workflowEnabledMap.get(workflow.id!)}
                        className={twMerge(
                            'cursor-pointer rounded-full border-2 border-transparent bg-gray-200 transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2',
                            workflowEnabledMap.get(workflow.id!) && 'bg-blue-600'
                        )}
                        onClick={() => {
                            setValue(
                                `projectInstanceWorkflows.${workflowIndex!}.enabled`,
                                !workflowEnabledMap.get(workflow.id!)
                            );
                            setWorkflowEnabled(workflow.id!, !workflowEnabledMap.get(workflow.id!));
                        }}
                    >
                        <span
                            aria-hidden="true"
                            className={twMerge(
                                'pointer-events-none inline-block size-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                                workflowEnabledMap.get(workflow.id!) ? 'translate-x-5' : 'translate-x-0'
                            )}
                        />
                    </Switch>
                </div>
            )}

            {workflow && (workflowEnabledMap.get(workflow.id!) || switchHidden) && (
                <div className="mt-2 space-y-6">
                    <div className="flex flex-col gap-3">
                        <Label className="font-semibold">Inputs</Label>

                        <ProjectInstanceDialogWorkflowsStepItemInputs
                            control={control}
                            formState={formState}
                            workflow={workflow}
                            workflowIndex={workflowIndex}
                        />
                    </div>

                    <div className="flex flex-col gap-3">
                        <Label className="font-semibold">Connections</Label>

                        <ProjectInstanceDialogWorkflowsStepItemConnections
                            control={control}
                            workflowConnections={workflowConnections}
                            workflowIndex={workflowIndex}
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

export default ProjectInstanceDialogWorkflowsStepItem;
