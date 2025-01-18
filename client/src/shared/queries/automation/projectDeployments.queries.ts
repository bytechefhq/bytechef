/* eslint-disable sort-keys */
import {
    GetWorkspaceProjectDeploymentsRequest,
    ProjectDeployment,
    ProjectDeploymentApi,
} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectDeploymentKeys = {
    filteredProjectDeployments: (filters: GetWorkspaceProjectDeploymentsRequest) => [
        ...ProjectDeploymentKeys.projectDeployments,
        filters.id,
        filters,
    ],
    projectDeployment: (id: number) => [...ProjectDeploymentKeys.projectDeployments, id],
    projectDeployments: ['projectDeployments'] as const,
};

export const useGetProjectDeploymentQuery = (id: number, enabled?: boolean) =>
    useQuery<ProjectDeployment, Error>({
        queryKey: ProjectDeploymentKeys.projectDeployment(id),
        queryFn: () => new ProjectDeploymentApi().getProjectDeployment({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetWorkspaceProjectDeploymentsQuery = (filters: GetWorkspaceProjectDeploymentsRequest) =>
    useQuery<ProjectDeployment[], Error>({
        queryKey: ProjectDeploymentKeys.filteredProjectDeployments(filters),
        queryFn: () => new ProjectDeploymentApi().getWorkspaceProjectDeployments(filters),
    });
