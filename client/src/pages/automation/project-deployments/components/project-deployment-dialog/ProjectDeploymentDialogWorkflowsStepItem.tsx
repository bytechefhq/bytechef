import {Label} from '@/components/ui/label';
import {Switch} from '@/components/ui/switch';
import ProjectDeploymentDialogWorkflowsStepItemConnections from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemConnections';
import ProjectDeploymentDialogWorkflowsStepItemInputs from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemInputs';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-deployments/stores/useWorkflowsEnabledStore';
import {ComponentConnection, ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {Control, FormState, UseFormSetValue} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

export interface ProjectDeploymentDialogWorkflowListItemProps {
    control: Control<ProjectDeployment>;
    formState: FormState<ProjectDeployment>;
    label?: string;
    setValue: UseFormSetValue<ProjectDeployment>;
    switchHidden?: boolean;
    workflow: Workflow;
    workflowIndex: number;
}

const ProjectDeploymentDialogWorkflowsStepItem = ({
    control,
    formState,
    label,
    setValue,
    switchHidden = false,
    workflow,
    workflowIndex,
}: ProjectDeploymentDialogWorkflowListItemProps) => {
    const [setWorkflowEnabled, workflowEnabledMap] = useWorkflowsEnabledStore(
        useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [setWorkflowEnabled, workflowEnabledMap])
    );

    const componentConnections: ComponentConnection[] = (workflow?.tasks ?? [])
        .flatMap((task) => task.connections ?? [])
        .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []));

    const workflowNodeLabelMap = new Map<string, string>();

    for (const task of workflow?.tasks ?? []) {
        if (task.label) {
            workflowNodeLabelMap.set(task.name, task.label);
        }
    }

    for (const trigger of workflow?.triggers ?? []) {
        if (trigger.label) {
            workflowNodeLabelMap.set(trigger.name, trigger.label);
        }
    }

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
                                `projectDeploymentWorkflows.${workflowIndex!}.enabled`,
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

            {(workflowEnabledMap.get(workflow.id!) || switchHidden) && (
                <ul className="mt-2 space-y-6">
                    <li className="flex flex-col gap-3">
                        <Label className="text-base font-semibold">Inputs</Label>

                        <ProjectDeploymentDialogWorkflowsStepItemInputs
                            control={control}
                            formState={formState}
                            workflow={workflow}
                            workflowIndex={workflowIndex}
                        />
                    </li>

                    <li className="flex flex-col gap-3">
                        <Label className="text-base font-semibold">Connections</Label>

                        <ProjectDeploymentDialogWorkflowsStepItemConnections
                            componentConnections={componentConnections}
                            control={control}
                            workflowIndex={workflowIndex}
                            workflowNodeLabelMap={workflowNodeLabelMap}
                        />
                    </li>
                </ul>
            )}
        </div>
    );
};

export default ProjectDeploymentDialogWorkflowsStepItem;
