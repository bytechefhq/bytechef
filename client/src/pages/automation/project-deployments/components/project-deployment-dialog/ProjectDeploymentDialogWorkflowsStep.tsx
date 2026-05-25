import ProjectDeploymentDialogWorkflowsStepItem from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem';
import {Connection, ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {Control, FormState, UseFormSetValue} from 'react-hook-form';

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
    const nonSubflowWorkflows = workflows.filter(
        (workflow) => !workflow.workflowTriggerComponentNames?.includes('workflow')
    );

    return (
        <div className="h-full space-y-5">
            {nonSubflowWorkflows?.map((workflow, workflowIndex) => (
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
                    workflowIndex={workflowIndex}
                    workflows={workflows}
                />
            ))}
        </div>
    );
};

export default ProjectDeploymentDialogWorkflowsStep;
