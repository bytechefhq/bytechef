import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import IntegrationInstanceConfigurationWorkflowExecutionsSheet from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-workflow-executions-sheet/IntegrationInstanceConfigurationWorkflowExecutionsSheet';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
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

    const environmentScopeNote = useEnvironmentScopeNote('MCP servers');

    const hasActiveFilter = filterData.id !== undefined;

    const isFirstRun = !mcpServersIsLoading && !hasActiveFilter && validMcpServers.length === 0;

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
                                uniqueIntegrations={uniqueIntegrations}
                            />
                        ) : (
                            isFirstRun && 'MCP Servers'
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
            leftSidebarHidden={isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[mcpServersError, tagsError]} loading={mcpServersIsLoading || tagsIsLoading}>
                {filteredMcpServers.length > 0 ? (
                    <WorkflowReadOnlyProvider value={{useGetComponentDefinitionsQuery}}>
                        <McpServerList mcpServers={filteredMcpServers as McpServer[]} tags={tags} />

                        <IntegrationInstanceConfigurationWorkflowExecutionsSheet />
                    </WorkflowReadOnlyProvider>
                ) : hasActiveFilter ? (
                    <EmptyList
                        icon={<ServerIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="No MCP servers match the selected filter."
                        title="No Matching MCP Servers"
                    />
                ) : (
                    <EmptyState
                        actions={
                            <McpServerDialog mcpServer={undefined} triggerNode={<Button label="Create MCP Server" />} />
                        }
                        description="An MCP server lets AI assistants, like Claude or Cursor, call tools for your connected users. You choose which component actions and integration workflows it exposes as tools."
                        icon={<ServerIcon />}
                        note={environmentScopeNote}
                        title="Create your first MCP server"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default McpServers;
