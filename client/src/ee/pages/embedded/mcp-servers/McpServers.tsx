import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {McpServer} from '@/shared/middleware/graphql';
import {ServerIcon} from 'lucide-react';

import McpServerDialog from './components/McpServerDialog';
import McpServersFilterTitle from './components/McpServersFilterTitle';
import McpServersLeftSidebarNav from './components/McpServersLeftSidebarNav';
import McpServerList from './components/mcp-server-list/McpServerList';
import useMcpServers from './hooks/useMcpServers';

export enum Type {
    Component,
    Integration,
    Tag,
}

const McpServers = () => {
    const canCreateMcpServer = useHasEmbeddedScope('MCP_CREATE');

    const {
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
    } = useMcpServers();

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        canCreateMcpServer &&
                        validMcpServers.length > 0 && (
                            <McpServerDialog mcpServer={undefined} triggerNode={<Button label="New MCP Server" />} />
                        )
                    }
                    title={
                        validMcpServers.length > 0 ? (
                            <McpServersFilterTitle
                                componentDefinitions={componentDefinitions}
                                filterData={filterData}
                                tags={tags}
                                uniqueIntegrations={uniqueIntegrations}
                            />
                        ) : (
                            ''
                        )
                    }
                />
            }
            leftSidebarBody={
                <McpServersLeftSidebarNav
                    allComponentNames={allComponentNames}
                    mcpServersIsLoading={mcpServersIsLoading}
                    validMcpServerIds={validMcpServerIds}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="MCP Servers" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[mcpServersError, tagsError]} loading={mcpServersIsLoading || tagsIsLoading}>
                {filteredMcpServers.length > 0 ? (
                    <McpServerList mcpServers={filteredMcpServers as McpServer[]} tags={tags} />
                ) : (
                    <EmptyList
                        button={
                            canCreateMcpServer ? (
                                <McpServerDialog
                                    mcpServer={undefined}
                                    triggerNode={<Button label="Create MCP Server" />}
                                />
                            ) : undefined
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
