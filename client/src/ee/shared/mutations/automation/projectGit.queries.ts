import {ProjectGitApi, ProjectGitConfiguration} from '@/ee/shared/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ProjectGitConfigurationKeys = {
    projectGitConfiguration: (id: number) => [...ProjectGitConfigurationKeys.projectGitConfigurations, id],
    projectGitConfigurations: ['projectGitConfigurations'] as const,
    projectRemoteBranches: (id: number) => [
        ...ProjectGitConfigurationKeys.projectGitConfigurations,
        'remoteBranches',
        id,
    ],
};

export const useGetProjectGitConfigurationQuery = (id: number) =>
    useQuery<ProjectGitConfiguration, Error>({
        queryKey: ProjectGitConfigurationKeys.projectGitConfiguration(id),
        queryFn: () => new ProjectGitApi().getProjectGitConfiguration({id}),
        retry: false,
    });

export const useGetProjectRemoteBranchesQuery = (id: number, enabled: boolean = true) =>
    useQuery<string[], Error>({
        queryKey: ProjectGitConfigurationKeys.projectRemoteBranches(id),
        queryFn: () => new ProjectGitApi().getProjectRemoteBranches({id}),
        enabled,
        retry: false,
    });

export const useGetWorkspaceProjectGitConfigurationsQuery = (workspaceId: number, enabled: boolean) =>
    useQuery<ProjectGitConfiguration[], Error>({
        queryKey: ProjectGitConfigurationKeys.projectGitConfigurations,
        queryFn: () => new ProjectGitApi().getWorkspaceProjectGitConfigurations({id: workspaceId}),
        enabled,
    });
