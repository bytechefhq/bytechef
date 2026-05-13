import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import ProjectDeploymentDialogWorkflowsStepItemInputs from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemInputs';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-deployments/stores/useWorkflowsEnabledStore';
import ConnectionConfigurationList from '@/shared/components/ConnectionConfigurationList';
import {Connection, ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {Control, FieldValues, FormState, UseFormSetValue} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

import getWorkflowComponentConnections from './projectDeploymentDialog-utils';

export interface ProjectDeploymentDialogWorkflowListItemProps {
    connections?: Connection[];
    connectionsGrouped?: boolean;
    control: Control<ProjectDeployment>;
    formState: FormState<ProjectDeployment>;
    label?: string;
    setValue: UseFormSetValue<ProjectDeployment>;
    switchHidden?: boolean;
    workflow: Workflow;
    workflowIndex: number;
}

const ProjectDeploymentDialogWorkflowsStepItem = ({
    connections,
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
                        <ConnectionConfigurationList
                            componentConnections={componentConnections}
                            connections={connections ?? []}
                            connectionsGrouped={connectionsGrouped}
                            control={control as unknown as Control<FieldValues>}
                            handleConnectionIdChange={(connectionIndex, connectionId) =>
                                setValue(
                                    `projectDeploymentWorkflows.${workflowIndex}.connections.${connectionIndex}.connectionId`,
                                    connectionId
                                )
                            }
                            workflow={workflow}
                        />
                    </li>
                </ul>
            )}
        </>
    );
};

export default ProjectDeploymentDialogWorkflowsStepItem;
