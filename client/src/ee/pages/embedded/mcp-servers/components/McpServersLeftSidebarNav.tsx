import {Type} from '@/ee/pages/embedded/mcp-servers/McpServers';
import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';
import {
    PlatformType,
    useMcpIntegrationInstanceConfigurationsQuery,
    useMcpServerTagsQuery,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {TagIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

interface McpServersLeftSidebarNavProps {
    allComponentNames: string[];
    mcpServersIsLoading?: boolean;
    validMcpServerIds: Set<string>;
}

const McpServersLeftSidebarNav = ({
    allComponentNames,
    mcpServersIsLoading = false,
    validMcpServerIds,
}: McpServersLeftSidebarNavProps) => {
    const [searchParams] = useSearchParams();

    const componentName = searchParams.get('componentName');
    const integrationId = searchParams.get('integrationId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: componentName ? componentName : integrationId ? integrationId : tagId ? tagId : undefined,
        type: componentName ? Type.Component : integrationId ? Type.Integration : tagId ? Type.Tag : Type.Component,
    };

    const {data: componentDefinitions, isLoading: componentDefinitionsIsLoading} = useGetComponentDefinitionsQuery({});

    const {data: mcpIntegrationInstanceConfigurationsData, isLoading: mcpIntegrationInstanceConfigurationsIsLoading} =
        useMcpIntegrationInstanceConfigurationsQuery();

    const {data: tagsData, isLoading: tagsIsLoading} = useMcpServerTagsQuery({type: PlatformType.Embedded});

    const tags = tagsData?.mcpServerTags;

    const mcpIntegrationInstanceConfigurations =
        mcpIntegrationInstanceConfigurationsData?.mcpIntegrationInstanceConfigurations?.filter(
            (integration): integration is NonNullable<typeof integration> => integration !== null
        ) || [];

    const serverMcpIntegrationInstanceConfigurations = mcpIntegrationInstanceConfigurations.filter((integration) =>
        validMcpServerIds.has(integration.mcpServerId)
    );

    const uniqueIntegrations = Array.from(
        new Map(
            serverMcpIntegrationInstanceConfigurations
                .filter((integration) => integration.integration?.id && integration.integration?.name)
                .map((integration) => [
                    integration.integration!.id,
                    {id: integration.integration!.id, name: integration.integration!.name},
                ])
        ).values()
    );

    const componentItems = (componentDefinitions ?? [])
        .filter((componentDefinition) => allComponentNames.includes(componentDefinition.name))
        .map((componentDefinition: ComponentDefinitionBasic) => ({
            current: filterData?.id === componentDefinition.name && filterData.type === Type.Component,
            id: componentDefinition.name,
            name: componentDefinition.title!,
            toLink: `?componentName=${componentDefinition.name}`,
        }));

    return (
        <>
            <LeftSidebarFilterNav
                items={componentItems}
                leadItem={{
                    current: !filterData?.id && filterData.type === Type.Component,
                    name: 'All Components',
                }}
                loading={componentDefinitionsIsLoading || mcpServersIsLoading}
                title="Components"
            />

            <LeftSidebarFilterNav
                emptyMessage="No integrations."
                items={uniqueIntegrations.map((integration) => ({
                    current: filterData?.id === integration.id && filterData.type === Type.Integration,
                    id: integration.id,
                    name: integration.name,
                    toLink: `?integrationId=${integration.id}`,
                }))}
                loading={mcpIntegrationInstanceConfigurationsIsLoading || mcpServersIsLoading}
                title="Integrations"
            />

            <LeftSidebarFilterNav
                emptyMessage="No defined tags."
                icon={<TagIcon className="mr-1 size-4" />}
                items={(tags ?? []).map((tag) => ({
                    current: filterData?.id === tag!.id && filterData.type === Type.Tag,
                    id: tag!.id,
                    name: tag!.name,
                    toLink: `?tagId=${tag!.id}`,
                }))}
                loading={tagsIsLoading}
                title="Tags"
            />
        </>
    );
};

export default McpServersLeftSidebarNav;
