import {Workflow} from '@/shared/middleware/automation/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {QueryClient} from '@tanstack/react-query';

interface UpdateCachedProjectWorkflowVersionI {
    projectId: number;
    version: number | undefined;
    workflowId: string | undefined;
}

export default function updateCachedProjectWorkflowVersion(
    queryClient: QueryClient,
    {projectId, version, workflowId}: UpdateCachedProjectWorkflowVersionI
) {
    void queryClient.invalidateQueries({exact: true, queryKey: ProjectKeys.project(projectId)});

    const queryKey = ProjectWorkflowKeys.projectWorkflows(projectId);

    const projectWorkflows = queryClient.getQueryData<Workflow[]>(queryKey);

    if (!workflowId || version === undefined || !projectWorkflows) {
        return;
    }

    queryClient.setQueryData<Workflow[]>(
        queryKey,
        projectWorkflows.map((projectWorkflow) =>
            projectWorkflow.id === workflowId ? {...projectWorkflow, version} : projectWorkflow
        )
    );
}
