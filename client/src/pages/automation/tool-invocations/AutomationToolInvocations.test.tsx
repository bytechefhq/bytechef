import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useToolInvocations} from '@/pages/automation/tool-invocations/hooks/useToolInvocations';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {act} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AutomationToolInvocations} from './AutomationToolInvocations';

const hoisted = vi.hoisted(() => ({
    useToolInvocationLogsQueryMock: vi.fn(),
    useWorkspaceMcpServersQueryMock: vi.fn(),
    useWorkspaceToolInvocationLogsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlatformType: {Automation: 'AUTOMATION', Embedded: 'EMBEDDED'},
    useToolInvocationLogsQuery: hoisted.useToolInvocationLogsQueryMock,
    useWorkspaceMcpServersQuery: hoisted.useWorkspaceMcpServersQueryMock,
    useWorkspaceToolInvocationLogsQuery: hoisted.useWorkspaceToolInvocationLogsQueryMock,
}));

vi.mock('@/pages/automation/tool-invocations/ToolInvocations', () => ({
    ToolInvocations: ({
        mcpServerOptions,
        workspaceScope,
    }: {
        mcpServerOptions: Array<{label: string; value: string}>;
        workspaceScope: Parameters<typeof useToolInvocations>[0];
    }) => {
        const {
            filterMcpServerId,
            filterOutcome,
            filterPageNumber,
            handleMcpServerChange,
            handleOutcomeChange,
            handlePaginationClick,
        } = useToolInvocations(workspaceScope);

        return (
            <div>
                <span data-testid="filters">
                    {`mcp=${filterMcpServerId ?? ''};outcome=${filterOutcome ?? ''};page=${filterPageNumber}`}
                </span>

                <button onClick={() => handleMcpServerChange({label: 'Server', value: '9'})}>pick server</button>

                <button onClick={() => handleOutcomeChange({label: 'Error', value: 'ERROR'})}>pick outcome</button>

                <button onClick={() => handlePaginationClick(3)}>next pages</button>

                <ul>
                    {mcpServerOptions.map((mcpServerOption) => (
                        <li data-testid="mcp-server-option" key={mcpServerOption.value}>
                            {`${mcpServerOption.value}:${mcpServerOption.label}`}
                        </li>
                    ))}
                </ul>
            </div>
        );
    },
}));

const emptyQueryResult = () => ({
    data: undefined,
    error: null,
    isFetching: false,
    isLoading: false,
    refetch: vi.fn(),
});

const renderPage = () =>
    render(
        <MemoryRouter>
            <AutomationToolInvocations />
        </MemoryRouter>
    );

describe('AutomationToolInvocations', () => {
    beforeEach(() => {
        hoisted.useToolInvocationLogsQueryMock.mockReset();
        hoisted.useToolInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());
        hoisted.useWorkspaceMcpServersQueryMock.mockReset();
        hoisted.useWorkspaceMcpServersQueryMock.mockReturnValue({data: undefined});
        hoisted.useWorkspaceToolInvocationLogsQueryMock.mockReset();
        hoisted.useWorkspaceToolInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());

        environmentStore.setState({currentEnvironmentId: 2});
        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it.each([
        ['environment', () => environmentStore.setState({currentEnvironmentId: 3}), '3', '7'],
        ['workspace', () => useWorkspaceStore.setState({currentWorkspaceId: 8}), '2', '8'],
    ])(
        'resets the filters and the page and queries the new scope when the %s changes',
        async (_scope, switchScope, environmentId, workspaceId) => {
            renderPage();

            await userEvent.click(screen.getByRole('button', {name: 'pick server'}));
            await userEvent.click(screen.getByRole('button', {name: 'pick outcome'}));
            await userEvent.click(screen.getByRole('button', {name: 'next pages'}));

            expect(screen.getByTestId('filters')).toHaveTextContent('mcp=9;outcome=ERROR;page=3');
            expect(hoisted.useWorkspaceToolInvocationLogsQueryMock).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    environmentId: '2',
                    mcpServerId: 9,
                    outcome: 'ERROR',
                    page: 3,
                    type: 'AUTOMATION',
                    workspaceId: '7',
                }),
                {enabled: true}
            );

            act(() => {
                switchScope();
            });

            expect(screen.getByTestId('filters')).toHaveTextContent('mcp=;outcome=;page=0');
            expect(hoisted.useWorkspaceToolInvocationLogsQueryMock).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    environmentId,
                    mcpServerId: undefined,
                    outcome: undefined,
                    page: 0,
                    type: 'AUTOMATION',
                    workspaceId,
                }),
                {enabled: true}
            );
        }
    );

    it('offers the MCP servers of the current workspace as filter options', () => {
        hoisted.useWorkspaceMcpServersQueryMock.mockReturnValue({
            data: {workspaceMcpServers: [{id: '11', name: 'Sales tools'}, null, {id: '12', name: 'Support tools'}]},
        });

        renderPage();

        expect(hoisted.useWorkspaceMcpServersQueryMock).toHaveBeenCalledWith({workspaceId: '7'}, {enabled: true});
        expect(screen.getAllByTestId('mcp-server-option').map((option) => option.textContent)).toEqual([
            '11:Sales tools',
            '12:Support tools',
        ]);
    });

    it('waits for a workspace before loading its MCP servers', () => {
        useWorkspaceStore.setState({currentWorkspaceId: undefined});

        renderPage();

        expect(hoisted.useWorkspaceMcpServersQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(screen.queryAllByTestId('mcp-server-option')).toHaveLength(0);
    });
});
