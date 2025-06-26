import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {ModeType, useMcpServersQuery} from '@/shared/middleware/graphql';
import {ServerIcon} from 'lucide-react';

import McpServerDialog from './components/McpServerDialog';
import McpServersLeftSidebarNav from './components/McpServersLeftSidebarNav';
import McpServerList from './components/mcp-server-list/McpServerList';

const McpServers = () => {
    const {
        data,
        error: mcpServersError,
        isLoading: mcpServersIsLoading,
    } = useMcpServersQuery({type: ModeType.Automation});

    if (!data || !data.mcpServers) {
        return <></>;
    }

    const validMcpServers = data.mcpServers.filter((server) => server !== null);

    return (
        <LayoutContainer
            header={
                validMcpServers.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={<McpServerDialog mcpServer={undefined} triggerNode={<Button>New MCP Server</Button>} />}
                        title="MCP Servers"
                    />
                )
            }
            leftSidebarBody={<McpServersLeftSidebarNav />}
            leftSidebarHeader={<Header position="sidebar" title="MCP Servers" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[mcpServersError]} loading={mcpServersIsLoading}>
                {validMcpServers.length > 0 ? (
                    <McpServerList mcpServers={validMcpServers} />
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
