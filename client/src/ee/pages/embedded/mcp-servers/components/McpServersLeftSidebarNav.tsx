import {Type} from '@/ee/pages/embedded/mcp-servers/McpServers';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {
    McpIntegration,
    PlatformType,
    useMcpIntegrationsQuery,
    useMcpServerTagsQuery,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {TagIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

interface McpServersLeftSidebarNavProps {
    allComponentNames: string[];
    validMcpServerIds: Set<string>;
}

const McpServersLeftSidebarNav = ({allComponentNames, validMcpServerIds}: McpServersLeftSidebarNavProps) => {
    const [searchParams] = useSearchParams();

    const componentName = searchParams.get('componentName');
    const integrationId = searchParams.get('integrationId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: componentName ? componentName : integrationId ? integrationId : tagId ? tagId : undefined,
        type: componentName ? Type.Component : integrationId ? Type.Integration : Type.Tag,
    };

    const {data: componentDefinitions, isLoading: componentDefinitionsIsLoading} = useGetComponentDefinitionsQuery({});

    const {data: mcpIntegrationsData, isLoading: mcpIntegrationsIsLoading} = useMcpIntegrationsQuery();

    const {data: tagsData, isLoading: tagsIsLoading} = useMcpServerTagsQuery({type: PlatformType.Embedded});

    const tags = tagsData?.mcpServerTags;

    const mcpIntegrations =
        mcpIntegrationsData?.mcpIntegrations?.filter(
            (integration): integration is McpIntegration => integration !== null
        ) || [];

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

    const filteredComponentDefinitions = useMemo(
        () =>
            componentDefinitions?.filter((componentDefinition) =>
                allComponentNames.includes(componentDefinition.name)
            ) ?? [],
        [componentDefinitions, allComponentNames]
    );

    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        <LeftSidebarNavItem
                            item={{
                                current: !filterData?.id && filterData.type === Type.Component,
                                name: 'All Components',
                            }}
                            toLink=""
                        />

                        {!componentDefinitionsIsLoading &&
                            filteredComponentDefinitions.map((componentDefinition: ComponentDefinitionBasic) => (
                                <LeftSidebarNavItem
                                    item={{
                                        current:
                                            filterData?.id === componentDefinition.name &&
                                            filterData.type === Type.Component,
                                        id: componentDefinition.name!,
                                        name: componentDefinition.title!,
                                    }}
                                    key={componentDefinition.name}
                                    toLink={`?componentName=${componentDefinition.name}`}
                                />
                            ))}
                    </>
                }
                title="Components"
            />

            <LeftSidebarNav
                body={
                    <>
                        {!mcpIntegrationsIsLoading && uniqueIntegrations.length === 0 && (
                            <span className="px-3 text-xs">No integrations.</span>
                        )}

                        {!mcpIntegrationsIsLoading &&
                            uniqueIntegrations.map((integration) => (
                                <LeftSidebarNavItem
                                    item={{
                                        current:
                                            filterData?.id === integration.id &&
                                            filterData.type === Type.Integration,
                                        id: integration.id!,
                                        name: integration.name,
                                    }}
                                    key={integration.id}
                                    toLink={`?integrationId=${integration.id}`}
                                />
                            ))}
                    </>
                }
                title="Integrations"
            />

            <LeftSidebarNav
                body={
                    <>
                        {!tagsIsLoading && !tags?.length && <span className="px-3 text-xs">No defined tags.</span>}

                        {!tagsIsLoading &&
                            tags?.map((tag) => (
                                <LeftSidebarNavItem
                                    icon={<TagIcon className="mr-1 size-4" />}
                                    item={{
                                        current: filterData?.id === tag!.id && filterData.type === Type.Tag,
                                        id: tag!.id,
                                        name: tag!.name,
                                    }}
                                    key={tag!.id}
                                    toLink={`?tagId=${tag!.id}`}
                                />
                            ))}
                    </>
                }
                title="Tags"
            />
        </>
    );
};

export default McpServersLeftSidebarNav;
