import {Type} from '@/ee/pages/embedded/mcp-servers/McpServers';
import {
    PlatformType,
    Tag,
    useEmbeddedMcpServersQuery,
    useMcpIntegrationsQuery,
    useMcpServerTagsQuery,
} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useSearchParams} from 'react-router-dom';

const useMcpServers = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [searchParams] = useSearchParams();

    const componentName = searchParams.get('componentName');
    const integrationId = searchParams.get('integrationId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: componentName ? componentName : integrationId ? integrationId : tagId ? tagId : undefined,
        type: componentName ? Type.Component : integrationId ? Type.Integration : tagId ? Type.Tag : Type.Component,
    };

    const {data, error: mcpServersError, isLoading: mcpServersIsLoading} = useEmbeddedMcpServersQuery();

    const {
        data: tagsData,
        error: tagsError,
        isLoading: tagsIsLoading,
    } = useMcpServerTagsQuery({type: PlatformType.Embedded});

    const {data: mcpIntegrationsData} = useMcpIntegrationsQuery();

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const validMcpServers = data?.embeddedMcpServers?.filter((server) => server !== null) || [];
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

    const mcpIntegrations = mcpIntegrationsData?.mcpIntegrations?.filter((integration) => integration !== null) || [];

    const serverMcpIntegrations = mcpIntegrations.filter((integration) =>
        validMcpServerIds.has(integration.mcpServerId)
    );

    const uniqueIntegrations = Array.from(
        new Map(
            serverMcpIntegrations
                .filter((integration) => integration.integration?.id && integration.integration?.name)
                .map((integration) => [
                    integration.integration!.id,
                    {id: integration.integration!.id, name: integration.integration!.name},
                ])
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

        if (integrationId) {
            const serverIntegrationIds = serverMcpIntegrations
                .filter((integration) => integration.mcpServerId === server.id)
                .map((integration) => integration.integration?.id);

            if (!serverIntegrationIds.includes(integrationId)) {
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
        filterData,
        filteredMcpServers,
        mcpServersError,
        mcpServersIsLoading,
        tags,
        tagsError,
        tagsIsLoading,
        uniqueIntegrations,
        validMcpServerIds,
        validMcpServers,
    };
};

export default useMcpServers;
