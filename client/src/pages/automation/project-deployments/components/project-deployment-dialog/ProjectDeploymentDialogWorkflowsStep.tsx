import ProjectDeploymentDialogWorkflowsStepItem from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem';
import {Connection, ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import compareWorkflowLabels from '@/shared/util/compareWorkflowLabels';
import {Control, FormState, UseFormSetValue, useWatch} from 'react-hook-form';

import {getReachableSubflowUuids} from './projectDeploymentDialog-utils';

export interface ProjectDeploymentDialogWorkflowsStepProps {
    connections?: Connection[];
    connectionsGrouped?: boolean;
    control: Control<ProjectDeployment>;
    formState: FormState<ProjectDeployment>;
    setValue: UseFormSetValue<ProjectDeployment>;
    workflows: Workflow[];
}

const ProjectDeploymentDialogWorkflowsStep = ({
    connections,
    connectionsGrouped,
    control,
    formState,
    setValue,
    workflows,
}: ProjectDeploymentDialogWorkflowsStepProps) => {
    const formWorkflows = useWatch({control, name: 'projectDeploymentWorkflows'});

    const getWorkflowIndex = (workflow: Workflow) => {
        const formWorkflowIndex =
            formWorkflows?.findIndex((formWorkflow) => formWorkflow.workflowId === workflow.id) ?? -1;

        return formWorkflowIndex === -1 ? workflows.indexOf(workflow) : formWorkflowIndex;
    };

    const reachableSubflowUuids = getReachableSubflowUuids(workflows, workflows);

    const nonSubflowWorkflows = workflows
        .filter((workflow) => !(workflow.workflowUuid && reachableSubflowUuids.has(workflow.workflowUuid)))
        .sort((workflow, otherWorkflow) => compareWorkflowLabels(workflow.label, otherWorkflow.label));

    return (
        <div className="min-h-full space-y-5">
            {nonSubflowWorkflows?.map((workflow) => (
                <ProjectDeploymentDialogWorkflowsStepItem
                    connections={connections}
                    connectionsGrouped={connectionsGrouped}
                    control={control}
                    formState={formState}
                    key={workflow.id!}
                    label={workflow.label!}
                    setValue={setValue}
                    showWorkflowToggle
                    workflow={workflow}
                    workflowIndex={getWorkflowIndex(workflow)}
                    workflows={workflows}
                />
            ))}
        </div>
    );
};

export default ProjectDeploymentDialogWorkflowsStep;
