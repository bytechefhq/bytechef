import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {Type} from '@/pages/automation/project-deployments/ProjectDeployments';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Environment, ModeType, Tag, useMcpServerTagsQuery, useMcpServersQuery} from '@/shared/middleware/graphql';
import {ServerIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

import McpServerDialog from './components/McpServerDialog';
import McpServersFilterTitle from './components/McpServersFilterTitle';
import McpServersLeftSidebarNav from './components/McpServersLeftSidebarNav';
import McpServerList from './components/mcp-server-list/McpServerList';

const McpServers = () => {
    const [searchParams] = useSearchParams();

    const environment = searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined;
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: tagId ? tagId : undefined,
        type: Type.Tag,
    };

    const {
        data,
        error: mcpServersError,
        isLoading: mcpServersIsLoading,
    } = useMcpServersQuery({type: ModeType.Automation});

    const {
        data: tagsData,
        error: tagsError,
        isLoading: tagsIsLoading,
    } = useMcpServerTagsQuery({type: ModeType.Automation});

    if (!data || !data.mcpServers) {
        return <></>;
    }

    const validMcpServers = data.mcpServers.filter((server) => server !== null);
    const tags = tagsData?.mcpServerTags as Tag[] | undefined;

    // Filter servers based on environment and/or tagId
    const filteredMcpServers = validMcpServers.filter((server) => {
        // Filter by environment if specified
        if (environment !== undefined) {
            const targetEnvironment =
                environment === 1
                    ? Environment.Development
                    : environment === 2
                      ? Environment.Staging
                      : Environment.Production;

            if (server.environment !== targetEnvironment) {
                return false;
            }
        }

        // Filter by tagId if specified
        if (tagId && server.tags) {
            const hasMatchingTag = server.tags.some((tag) => tag?.id === tagId);
            if (!hasMatchingTag) {
                return false;
            }
        }

        return true;
    });

    return (
        <LayoutContainer
            header={
                validMcpServers.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={<McpServerDialog mcpServer={undefined} triggerNode={<Button>New MCP Server</Button>} />}
                        title={<McpServersFilterTitle environment={environment} filterData={filterData} tags={tags} />}
                    />
                )
            }
            leftSidebarBody={<McpServersLeftSidebarNav />}
            leftSidebarHeader={<Header position="sidebar" title="MCP Servers" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[mcpServersError, tagsError]} loading={mcpServersIsLoading || tagsIsLoading}>
                {filteredMcpServers.length > 0 ? (
                    <McpServerList mcpServers={filteredMcpServers} tags={tags} />
                ) : (
                    <EmptyList
                        button={
                            <McpServerDialog mcpServer={undefined} triggerNode={<Button>Create MCP Server</Button>} />
                        }
                        icon={<ServerIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new MCP server."
                        title="No MCP Servers"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default McpServers;
