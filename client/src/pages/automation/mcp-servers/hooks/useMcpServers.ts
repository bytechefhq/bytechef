import {Type} from '@/pages/automation/mcp-servers/McpServers';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    PlatformType,
    Tag,
    useMcpProjectsQuery,
    useMcpServerTagsQuery,
    useWorkspaceMcpServersQuery,
} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useSearchParams} from 'react-router-dom';

const useMcpServers = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [searchParams] = useSearchParams();

    const componentName = searchParams.get('componentName');
    const projectId = searchParams.get('projectId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: componentName ? componentName : projectId ? projectId : tagId ? tagId : undefined,
        type: componentName ? Type.Component : projectId ? Type.Project : tagId ? Type.Tag : Type.Component,
    };

    const {
        data,
        error: mcpServersError,
        isLoading: mcpServersIsLoading,
    } = useWorkspaceMcpServersQuery({workspaceId: currentWorkspaceId + ''});

    const {
        data: tagsData,
        error: tagsError,
        isLoading: tagsIsLoading,
    } = useMcpServerTagsQuery({type: PlatformType.Automation});

    const {data: mcpProjectsData, isLoading: mcpProjectsIsLoading} = useMcpProjectsQuery();

    const {data: componentDefinitions, isLoading: componentDefinitionsIsLoading} = useGetComponentDefinitionsQuery({});

    const validMcpServers = data?.workspaceMcpServers?.filter((server) => server !== null) || [];
    const tags = tagsData?.mcpServerTags as Tag[] | undefined;
    const validMcpServerIds = new Set(validMcpServers.map((server) => server.id));

    const allComponentNames = Array.from(
        new Set(
            validMcpServers
                .flatMap((server) => server.mcpComponents || [])
                .map((component) => component?.componentName)
                .filter((name): name is string => !!name)
        )
    );

    const mcpProjects =
        mcpProjectsData?.mcpProjects?.filter((project): project is NonNullable<typeof project> => project !== null) ||
        [];

    const workspaceMcpProjects = mcpProjects.filter((project) => validMcpServerIds.has(project.mcpServerId));

    const uniqueProjects = Array.from(
        new Map(
            workspaceMcpProjects
                .filter((project) => project.project?.id && project.project?.name)
                .map(
                    (project) => [project.project!.id, {id: project.project!.id, name: project.project!.name}] as const
                )
        ).values()
    );

    const filteredMcpServers = validMcpServers.filter((server) => {
        if (+server.environmentId !== currentEnvironmentId) {
            return false;
        }

        if (componentName) {
            if (!server.mcpComponents?.some((mcpComponent) => mcpComponent?.componentName === componentName)) {
                return false;
            }
        }

        if (projectId) {
            const serverProjectIds = workspaceMcpProjects
                .filter((project) => project.mcpServerId === server.id)
                .map((project) => project.project?.id);

            if (!serverProjectIds.includes(projectId)) {
                return false;
            }
        }

        if (tagId && server.tags) {
            const hasMatchingTag = server.tags.some((tag) => tag?.id === tagId);

            if (!hasMatchingTag) {
                return false;
            }
        }

        return true;
    });

    return {
        allComponentNames,
        componentDefinitions,
        componentDefinitionsIsLoading,
        filterData,
        filteredMcpServers,
        mcpProjectsIsLoading,
        mcpServersError,
        mcpServersIsLoading,
        tags,
        tagsError,
        tagsIsLoading,
        uniqueProjects,
        validMcpServerIds,
        validMcpServers,
    };
};

export default useMcpServers;
