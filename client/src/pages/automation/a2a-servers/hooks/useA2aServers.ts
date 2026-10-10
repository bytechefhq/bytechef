import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Tag, useA2aProjectsQuery, useA2aServerTagsQuery, useA2aServersQuery} from '@/shared/middleware/graphql';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

export enum A2aServersFilterType {
    Project,
    Tag,
}

const useA2aServers = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [searchParams] = useSearchParams();

    const projectId = searchParams.get('projectId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: projectId || tagId || undefined,
        type: tagId ? A2aServersFilterType.Tag : A2aServersFilterType.Project,
    };

    const {data, error: a2aServersError, isLoading: a2aServersIsLoading} = useA2aServersQuery();

    const {data: tagsData, error: tagsError, isLoading: tagsIsLoading} = useA2aServerTagsQuery();

    const {data: a2aProjectsData, isLoading: a2aProjectsIsLoading} = useA2aProjectsQuery();

    const {data: projects} = useGetWorkspaceProjectsQuery({
        apiCollections: false,
        id: currentWorkspaceId!,
        includeAllFields: false,
    });

    const a2aServers = useMemo(
        () =>
            (data?.a2aServers ?? []).filter(
                (a2aServer): a2aServer is NonNullable<typeof a2aServer> =>
                    a2aServer != null && Number(a2aServer.environmentId) === currentEnvironmentId
            ),
        [data, currentEnvironmentId]
    );

    const tags = tagsData?.a2aServerTags?.filter((tag): tag is Tag => tag != null);

    const a2aProjects = useMemo(() => {
        const a2aServerIds = new Set(a2aServers.map((a2aServer) => a2aServer.id));

        return (a2aProjectsData?.a2aProjects ?? []).filter(
            (a2aProject): a2aProject is NonNullable<typeof a2aProject> =>
                a2aProject != null && a2aServerIds.has(a2aProject.a2aServerId)
        );
    }, [a2aProjectsData, a2aServers]);

    const uniqueProjects = useMemo(() => {
        const projectNameById = new Map(projects?.map((project) => [String(project.id), project.name]));

        return Array.from(
            new Map(
                a2aProjects
                    .filter((a2aProject) => a2aProject.projectId && projectNameById.has(a2aProject.projectId))
                    .map((a2aProject) => {
                        const id = a2aProject.projectId!;

                        return [id, {id, name: projectNameById.get(id)!}] as const;
                    })
            ).values()
        );
    }, [a2aProjects, projects]);

    const filteredA2aServers = useMemo(
        () =>
            a2aServers.filter((a2aServer) => {
                if (projectId) {
                    const hasMatchingProject = a2aProjects.some(
                        (a2aProject) => a2aProject.a2aServerId === a2aServer.id && a2aProject.projectId === projectId
                    );

                    if (!hasMatchingProject) {
                        return false;
                    }
                }

                if (tagId) {
                    const hasMatchingTag = (a2aServer.tags ?? []).some((tag) => tag?.id === tagId);

                    if (!hasMatchingTag) {
                        return false;
                    }
                }

                return true;
            }),
        [a2aProjects, a2aServers, projectId, tagId]
    );

    return {
        a2aProjectsIsLoading,
        a2aServers,
        a2aServersError,
        a2aServersIsLoading,
        filterData,
        filteredA2aServers,
        tags,
        tagsError,
        tagsIsLoading,
        uniqueProjects,
    };
};

export default useA2aServers;
