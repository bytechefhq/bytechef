import {useExternalInvocations} from '@/pages/automation/external-invocations/hooks/useExternalInvocations';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {act} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AutomationExternalInvocations} from './AutomationExternalInvocations';

const hoisted = vi.hoisted(() => ({
    grantedScopes: new Set<string>(),
    useA2aServersQueryMock: vi.fn(),
    useExternalInvocationLogsQueryMock: vi.fn(),
    useWorkspaceExternalInvocationLogsQueryMock: vi.fn(),
    useWorkspaceMcpServersQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlatformType: {Automation: 'AUTOMATION', Embedded: 'EMBEDDED'},
    useA2aServersQuery: hoisted.useA2aServersQueryMock,
    useEnvironmentsQuery: () => ({data: undefined}),
    useExternalInvocationLogsQuery: hoisted.useExternalInvocationLogsQueryMock,
    useWorkspaceExternalInvocationLogsQuery: hoisted.useWorkspaceExternalInvocationLogsQueryMock,
    useWorkspaceMcpServersQuery: hoisted.useWorkspaceMcpServersQueryMock,
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) => hoisted.grantedScopes.has(scope),
}));

vi.mock('@/shared/hooks/useLoadWorkspaceScopes', () => ({
    useLoadWorkspaceScopes: () => ({error: false, loading: false}),
}));

vi.mock('@/pages/automation/external-invocations/ExternalInvocations', () => ({
    ExternalInvocations: ({
        a2aServerOptions,
        firstRunEmptyState,
        mcpServerOptions,
        type,
        workspaceScope,
    }: {
        a2aServerOptions: Array<{label: string; value: string}>;
        firstRunEmptyState: ReactNode;
        mcpServerOptions: Array<{label: string; value: string}>;
    } & Parameters<typeof useExternalInvocations>[0]) => {
        const {
            filterA2aServerId,
            filterMcpServerId,
            filterOutcome,
            filterPageNumber,
            handleA2aServerChange,
            handleMcpServerChange,
            handleOutcomeChange,
            handlePaginationClick,
        } = useExternalInvocations({type, workspaceScope});

        return (
            <div>
                <span data-testid="filters">
                    {`a2a=${filterA2aServerId ?? ''};mcp=${filterMcpServerId ?? ''};outcome=${filterOutcome ?? ''};page=${filterPageNumber}`}
                </span>

                <button onClick={() => handleA2aServerChange({label: 'Agent', value: '4'})}>pick a2a server</button>

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

                <ul>
                    {a2aServerOptions.map((a2aServerOption) => (
                        <li data-testid="a2a-server-option" key={a2aServerOption.value}>
                            {`${a2aServerOption.value}:${a2aServerOption.label}`}
                        </li>
                    ))}
                </ul>

                {firstRunEmptyState}
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
            <AutomationExternalInvocations />
        </MemoryRouter>
    );

describe('AutomationExternalInvocations', () => {
    beforeEach(() => {
        hoisted.grantedScopes = new Set(['A2A_VIEW', 'MCP_VIEW']);
        hoisted.useA2aServersQueryMock.mockReset();
        hoisted.useA2aServersQueryMock.mockReturnValue({data: undefined});
        hoisted.useExternalInvocationLogsQueryMock.mockReset();
        hoisted.useExternalInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());
        hoisted.useWorkspaceMcpServersQueryMock.mockReset();
        hoisted.useWorkspaceMcpServersQueryMock.mockReturnValue({data: undefined});
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReset();
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());

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

            expect(screen.getByTestId('filters')).toHaveTextContent('a2a=;mcp=9;outcome=ERROR;page=3');
            expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
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

            expect(screen.getByTestId('filters')).toHaveTextContent('a2a=;mcp=;outcome=;page=0');
            expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
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

    it('offers the A2A servers of the current workspace as filter options', () => {
        hoisted.useA2aServersQueryMock.mockReturnValue({
            data: {a2aServers: [{id: '4', name: 'Research agent'}, null, {id: '5', name: 'Billing agent'}]},
        });

        renderPage();

        expect(hoisted.useA2aServersQueryMock).toHaveBeenCalledWith({workspaceId: '7'}, {enabled: true});
        expect(screen.getAllByTestId('a2a-server-option').map((option) => option.textContent)).toEqual([
            '4:Research agent',
            '5:Billing agent',
        ]);
    });

    it.each([
        ['A2A_VIEW', () => hoisted.useA2aServersQueryMock, () => hoisted.useWorkspaceMcpServersQueryMock],
        ['MCP_VIEW', () => hoisted.useWorkspaceMcpServersQueryMock, () => hoisted.useA2aServersQueryMock],
    ])(
        'does not query the servers a user without the %s scope may not view',
        (scope, getDeniedQueryMock, getAllowedQueryMock) => {
            hoisted.grantedScopes.delete(scope);

            renderPage();

            expect(getDeniedQueryMock()).toHaveBeenCalledWith({workspaceId: '7'}, {enabled: false});
            expect(getAllowedQueryMock()).toHaveBeenCalledWith({workspaceId: '7'}, {enabled: true});
        }
    );

    it('queries the external invocations of the picked A2A server', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('button', {name: 'pick a2a server'}));

        expect(screen.getByTestId('filters')).toHaveTextContent('a2a=4;mcp=;outcome=;page=0');
        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({a2aServerId: 4, mcpServerId: undefined, type: 'AUTOMATION'}),
            {enabled: true}
        );
    });

    it('waits for a workspace before loading its MCP servers', () => {
        useWorkspaceStore.setState({currentWorkspaceId: undefined});

        renderPage();

        expect(hoisted.useWorkspaceMcpServersQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(screen.queryAllByTestId('mcp-server-option')).toHaveLength(0);
    });

    it('guides a new user to create an MCP server when the environment has none', () => {
        hoisted.useWorkspaceMcpServersQueryMock.mockReturnValue({
            data: {workspaceMcpServers: [{environmentId: '3', id: '11', name: 'Sales tools'}]},
        });

        renderPage();

        expect(screen.getByText('Create an MCP server').closest('li')).toHaveAttribute('aria-current', 'step');
        expect(screen.getByText('Connect an AI client').closest('li')).not.toHaveAttribute('aria-current');
    });

    it('moves the guide on to connecting an AI client once the environment has an MCP server', () => {
        hoisted.useWorkspaceMcpServersQueryMock.mockReturnValue({
            data: {workspaceMcpServers: [{environmentId: '2', id: '11', name: 'Sales tools'}]},
        });

        renderPage();

        expect(screen.getByText('Completed:')).toBeInTheDocument();
        expect(screen.getByText('Connect an AI client').closest('li')).toHaveAttribute('aria-current', 'step');
    });
});
