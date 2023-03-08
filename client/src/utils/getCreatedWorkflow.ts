import {WorkflowModel} from 'middleware/workflow';
import {IProjectWithWorkflows} from 'mutations/projects.mutations';

export default function getCreatedWorkflow(
    projectWorkflows: WorkflowModel[],
    project: IProjectWithWorkflows
): WorkflowModel | undefined {
    const staleProjectWorkflowIds = projectWorkflows?.map(
        (workflow) => workflow.id
    );

    const {workflows, workflowIds} = project;

    const createdWorkflowId = workflowIds?.find(
        (workflowId) => !staleProjectWorkflowIds?.includes(workflowId)
    );

    return workflows.find((workflow) => workflow.id === createdWorkflowId);
}
