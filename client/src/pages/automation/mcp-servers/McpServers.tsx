import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {McpServer} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ServerIcon} from 'lucide-react';
import {useEffect} from 'react';

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

    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    // Refresh the server list and the linked projects after a BUILD-mode copilot turn creates or modifies an
    // MCP server, so the page reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.MCP_SERVER, () => {
            queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
            queryClient.invalidateQueries({queryKey: ['mcpProjects']});
            queryClient.invalidateQueries({queryKey: ['mcpProjectsByServerId']});
        });
    }, [queryClient, registerPostTurn]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        (validMcpServers.length > 0 || !(mcpServersIsLoading || tagsIsLoading)) && (
                            <div className="flex items-center gap-1">
                                <CopilotButton source={Source.MCP_SERVER} />

                                {validMcpServers.length > 0 && (
                                    <McpServerDialog
                                        mcpServer={undefined}
                                        triggerNode={<Button label="New MCP Server" />}
                                    />
                                )}
                            </div>
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
                ) : (
                    <EmptyList
                        button={
                            <McpServerDialog mcpServer={undefined} triggerNode={<Button label="Create MCP Server" />} />
                        }
                        icon={<ServerIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new MCP server."
                        title="No MCP Servers"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default McpServers;
