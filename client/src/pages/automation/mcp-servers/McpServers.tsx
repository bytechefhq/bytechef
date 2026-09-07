import Button from '@/components/Button/Button';
import EmptyFilterResult from '@/components/EmptyFilterResult';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
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
    Project,
    Tag,
}

const McpServers = () => {
    const {
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
        validMcpServers,
    } = useMcpServers();

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
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
                                uniqueProjects={uniqueProjects}
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
                    componentDefinitions={componentDefinitions}
                    componentDefinitionsIsLoading={componentDefinitionsIsLoading}
                    filterData={filterData}
                    mcpProjectsIsLoading={mcpProjectsIsLoading}
                    mcpServersIsLoading={mcpServersIsLoading}
                    tags={tags}
                    tagsIsLoading={tagsIsLoading}
                    uniqueProjects={uniqueProjects}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="MCP Servers" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[mcpServersError, tagsError]} loading={mcpServersIsLoading || tagsIsLoading}>
                {filteredMcpServers.length > 0 ? (
                    <McpServerList mcpServers={filteredMcpServers as McpServer[]} tags={tags} />
                ) : validMcpServers.length > 0 ? (
                    <EmptyFilterResult entityName="MCP servers" entityTitle="MCP Servers" />
                ) : (
                    <EmptyList
                        button={
                            <McpServerDialog mcpServer={undefined} triggerNode={<Button label="Create MCP Server" />} />
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
