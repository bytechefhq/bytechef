import {ProjectGitApi, ProjectGitConfiguration} from '@/ee/shared/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ProjectGitConfigurationKeys = {
    projectGitConfiguration: (id: number) => [...ProjectGitConfigurationKeys.projectGitConfigurations, id],
    projectGitConfigurations: ['projectGitConfigurations'] as const,
};

export const useGetProjectGitConfigurationQuery = (id: number) =>
    useQuery<ProjectGitConfiguration, Error>({
        queryKey: ProjectGitConfigurationKeys.projectGitConfiguration(id),
        queryFn: () => new ProjectGitApi().getProjectGitConfiguration({id}),
    });

export const useGetWorkspaceProjectGitConfigurationsQuery = (workspaceId: number, enabled: boolean) =>
    useQuery<ProjectGitConfiguration[], Error>({
        queryKey: ProjectGitConfigurationKeys.projectGitConfigurations,
        queryFn: () => new ProjectGitApi().getWorkspaceProjectGitConfigurations({id: workspaceId}),
        enabled,
    });
