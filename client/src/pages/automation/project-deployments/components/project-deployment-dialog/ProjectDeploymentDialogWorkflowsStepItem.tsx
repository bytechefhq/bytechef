import {useWorkflowsEnabledStore} from '@/pages/automation/project-deployments/stores/useWorkflowsEnabledStore';
import ConnectionConfigurationList from '@/shared/components/ConnectionConfigurationList';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import WorkflowConfigurationCard from '@/shared/components/workflow-configuration/WorkflowConfigurationCard';
import {Connection, ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {Control, FieldValues, FormState, UseFormSetValue, useWatch} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

import getWorkflowComponentConnections, {
    getSubflowConnectionStubs,
    getSubflowInputStubs,
    getWorkflowInputs,
} from './projectDeploymentDialog-utils';

export interface ProjectDeploymentDialogWorkflowListItemProps {
    connections?: Connection[];
    connectionsGrouped?: boolean;
    control: Control<ProjectDeployment>;
    enabled?: boolean;
    formState: FormState<ProjectDeployment>;
    label?: string;
    onEnabledChange?: (enabled: boolean) => void;
    setValue: UseFormSetValue<ProjectDeployment>;
    showWorkflowToggle?: boolean;
    workflow: Workflow;
    workflowIndex: number;
    workflows: Workflow[];
}

const ProjectDeploymentDialogWorkflowsStepItem = ({
    connections,
    connectionsGrouped,
    control,
    enabled,
    formState,
    label,
    onEnabledChange,
    setValue,
    showWorkflowToggle = false,
    workflow,
    workflowIndex,
    workflows,
}: ProjectDeploymentDialogWorkflowListItemProps) => {
    const [setWorkflowEnabled, workflowEnabledMap] = useWorkflowsEnabledStore(
        useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [setWorkflowEnabled, workflowEnabledMap])
    );

    const componentConnections = getWorkflowComponentConnections(workflow, workflows);
    const workflowInputs = getWorkflowInputs(workflow, workflows);
    const duplicateSubflowConnectionStubs = getSubflowConnectionStubs(workflow, workflows);
    const duplicateSubflowInputStubs = getSubflowInputStubs(workflow, workflows);
    const subflowLabelMap = new Map<string, string>();

    for (const subflowWorkflow of workflows) {
        if (subflowWorkflow.workflowUuid && subflowWorkflow.label) {
            subflowLabelMap.set(subflowWorkflow.workflowUuid, subflowWorkflow.label);
        }
    }

    const watchedConnections = useWatch({
        control,
        name: `projectDeploymentWorkflows.${workflowIndex}.connections`,
    });

    const workflowEnabled = enabled ?? workflowEnabledMap.get(workflow.id!);

    const handleEnabledChange = (value: boolean) => {
        if (onEnabledChange) {
            onEnabledChange(value);

            return;
        }

        setValue(`projectDeploymentWorkflows.${workflowIndex!}.enabled`, value);

        setWorkflowEnabled(workflow.id!, value);
    };

    return (
        <WorkflowConfigurationCard
            connectionCount={componentConnections.length}
            connectionsContent={
                <ConnectionConfigurationList
                    componentConnections={componentConnections}
                    connections={connections ?? []}
                    connectionsGrouped={connectionsGrouped}
                    control={control as unknown as Control<FieldValues>}
                    duplicateSubflowStubs={duplicateSubflowConnectionStubs}
                    fieldNamePrefix={`projectDeploymentWorkflows.${workflowIndex}.connections`}
                    getCurrentConnectionId={(connectionIndex) => watchedConnections?.[connectionIndex]?.connectionId}
                    handleConnectionIdChange={(connectionIndex, connectionId) =>
                        setValue(
                            `projectDeploymentWorkflows.${workflowIndex}.connections.${connectionIndex}.connectionId`,
                            connectionId
                        )
                    }
                    subflowLabelMap={subflowLabelMap}
                    workflow={workflow}
                />
            }
            enabled={!!workflowEnabled}
            inputCount={workflowInputs.length}
            inputsContent={
                <InputConfigurationList
                    control={control as unknown as Control<FieldValues>}
                    controlPath={`projectDeploymentWorkflows.${workflowIndex}.inputs`}
                    duplicateSubflowStubs={duplicateSubflowInputStubs}
                    formState={formState as unknown as FormState<FieldValues>}
                    inputs={workflowInputs}
                    subflowLabelMap={subflowLabelMap}
                />
            }
            label={label}
            onEnabledChange={handleEnabledChange}
            showWorkflowToggle={showWorkflowToggle}
            switchId={`projectDeploymentWorkflows.${workflowIndex}`}
        />
    );
};

export default ProjectDeploymentDialogWorkflowsStepItem;
