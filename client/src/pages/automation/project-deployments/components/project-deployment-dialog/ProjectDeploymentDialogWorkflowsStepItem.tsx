import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import ProjectDeploymentDialogWorkflowsStepItemConnections from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemConnections';
import ProjectDeploymentDialogWorkflowsStepItemInputs from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemInputs';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-deployments/stores/useWorkflowsEnabledStore';
import {ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {Control, FormState, UseFormSetValue} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

import getWorkflowComponentConnections from './projectDeploymentDialog-utils';

export interface ProjectDeploymentDialogWorkflowListItemProps {
    control: Control<ProjectDeployment>;
    formState: FormState<ProjectDeployment>;
    connectionsGrouped?: boolean;
    label?: string;
    setValue: UseFormSetValue<ProjectDeployment>;
    switchHidden?: boolean;
    workflow: Workflow;
    workflowIndex: number;
}

const ProjectDeploymentDialogWorkflowsStepItem = ({
    connectionsGrouped,
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

    const componentConnections = getWorkflowComponentConnections(workflow);

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
        <>
            {!switchHidden && (
                <div className="flex cursor-pointer justify-between py-2">
                    <span className="font-semibold">{label}</span>

                    <Switch
                        checked={workflowEnabledMap.get(workflow.id!)}
                        onCheckedChange={(value) => {
                            setValue(`projectDeploymentWorkflows.${workflowIndex!}.enabled`, value);
                            setWorkflowEnabled(workflow.id!, value);
                        }}
                    />
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
                            connectionsGrouped={connectionsGrouped}
                            control={control}
                            setValue={setValue}
                            workflowIndex={workflowIndex}
                            workflowNodeLabelMap={workflowNodeLabelMap}
                        />
                    </li>
                </ul>
            )}
        </>
    );
};

export default ProjectDeploymentDialogWorkflowsStepItem;
