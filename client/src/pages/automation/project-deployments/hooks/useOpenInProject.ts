import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {ProjectApi, WorkflowApi} from '@/shared/middleware/automation/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

const PROJECTS_HREF = '/automation/projects';

export default function useOpenInProject({onBeforeNavigate}: {onBeforeNavigate?: () => void} = {}) {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const canOpenInProject = currentEnvironmentId === DEVELOPMENT_ENVIRONMENT;

    const openProject = useCallback(
        async (projectId: number) => {
            try {
                const project = await queryClient.fetchQuery({
                    queryFn: () => new ProjectApi().getProject({id: projectId}),
                    queryKey: ProjectKeys.project(projectId),
                });

                const firstProjectWorkflowId = project.projectWorkflowIds?.[0];

                if (firstProjectWorkflowId == null) {
                    toast('The project has no workflows to open.');

                    return;
                }

                onBeforeNavigate?.();

                navigate(`${PROJECTS_HREF}/${projectId}/project-workflows/${firstProjectWorkflowId}`);
            } catch {
                return;
            }
        },
        [navigate, onBeforeNavigate, queryClient]
    );

    const openProjectWorkflow = useCallback(
        async (projectId: number, workflowUuid?: string) => {
            try {
                const projectWorkflows = await queryClient.fetchQuery({
                    queryFn: () => new WorkflowApi().getProjectWorkflows({id: projectId}),
                    queryKey: ProjectWorkflowKeys.projectWorkflows(projectId),
                });

                const matchingWorkflow = projectWorkflows.find(
                    (projectWorkflow) => workflowUuid != null && projectWorkflow.workflowUuid === workflowUuid
                );

                if (matchingWorkflow?.projectWorkflowId == null) {
                    toast('This workflow no longer exists in the latest project version.');

                    return;
                }

                onBeforeNavigate?.();

                navigate(`${PROJECTS_HREF}/${projectId}/project-workflows/${matchingWorkflow.projectWorkflowId}`);
            } catch {
                return;
            }
        },
        [navigate, onBeforeNavigate, queryClient]
    );

    return {canOpenInProject, openProject, openProjectWorkflow};
}
