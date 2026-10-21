import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {act} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppActivityTab from '../components/AppActivityTab';

const hoisted = vi.hoisted(() => ({
    useWorkspaceExternalInvocationLogsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlatformType: {Automation: 'AUTOMATION', Embedded: 'EMBEDDED'},
    useWorkspaceExternalInvocationLogsQuery: hoisted.useWorkspaceExternalInvocationLogsQueryMock,
}));

vi.mock('@/pages/automation/external-invocations/components/ExternalInvocationsTable', () => ({
    default: ({externalInvocationLogs}: {externalInvocationLogs: Array<{id: string; invocationName: string}>}) => (
        <ul>
            {externalInvocationLogs.map((externalInvocationLog) => (
                <li key={externalInvocationLog.id}>{externalInvocationLog.invocationName}</li>
            ))}
        </ul>
    ),
}));

const renderTab = () =>
    render(
        <MemoryRouter>
            <AppActivityTab appId="1" />
        </MemoryRouter>
    );

describe('AppActivityTab', () => {
    beforeEach(() => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReset();
        environmentStore.setState({currentEnvironmentId: 2});
        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('queries the invocations of the app and lists them', () => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue({
            data: {
                workspaceExternalInvocationLogs: {
                    content: [
                        {id: '1', invocationName: 'SALES_SLACK__SEND_MESSAGE'},
                        {id: '2', invocationName: 'SALES_SLACK__LIST_CHANNELS'},
                    ],
                    number: 0,
                    size: 20,
                    totalElements: 2,
                    totalPages: 1,
                },
            },
            isLoading: false,
        });

        renderTab();

        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenCalledWith(
            {appId: 1, environmentId: '2', page: 0, type: 'AUTOMATION', workspaceId: '7'},
            {enabled: true}
        );
        expect(screen.getByText('SALES_SLACK__SEND_MESSAGE')).toBeInTheDocument();
        expect(screen.getByText('SALES_SLACK__LIST_CHANNELS')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Load more'})).not.toBeInTheDocument();
    });

    it('shows the empty message when the app has no invocations', () => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue({
            data: {
                workspaceExternalInvocationLogs: {content: [], number: 0, size: 20, totalElements: 0, totalPages: 0},
            },
            isLoading: false,
        });

        renderTab();

        expect(screen.getByText('No invocations recorded for this app yet.')).toBeInTheDocument();
    });

    it('does not query for a malformed app id', () => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue({data: undefined, isLoading: false});

        render(
            <MemoryRouter>
                <AppActivityTab appId="abc" />
            </MemoryRouter>
        );

        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).not.toHaveBeenCalled();
        expect(screen.getByText('No invocations recorded for this app yet.')).toBeInTheDocument();
    });

    it('links to the External Invocations page filtered by the app', () => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue({
            data: {
                workspaceExternalInvocationLogs: {content: [], number: 0, size: 20, totalElements: 0, totalPages: 0},
            },
            isLoading: false,
        });

        renderTab();

        expect(screen.getByRole('link', {name: /View in External Invocations/})).toHaveAttribute(
            'href',
            '/automation/external-invocations?appId=1'
        );
    });

    it('loads the next page when there are more pages', async () => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue({
            data: {
                workspaceExternalInvocationLogs: {
                    content: [{id: '1', invocationName: 'SALES_SLACK__SEND_MESSAGE'}],
                    number: 0,
                    size: 1,
                    totalElements: 2,
                    totalPages: 2,
                },
            },
            isLoading: false,
        });

        renderTab();

        await userEvent.click(screen.getByRole('button', {name: 'Load more'}));

        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            {appId: 1, environmentId: '2', page: 1, type: 'AUTOMATION', workspaceId: '7'},
            {enabled: true}
        );
    });

    it.each([
        ['environment', () => environmentStore.setState({currentEnvironmentId: 3}), '3', '7'],
        ['workspace', () => useWorkspaceStore.setState({currentWorkspaceId: 8}), '2', '8'],
    ])(
        'drops the loaded pages and returns to page 0 when the %s changes',
        async (_scope, switchScope, environmentId, workspaceId) => {
            const resultsByVariables = new Map<string, object>();

            hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockImplementation(
                (variables: {environmentId: string; page: number; workspaceId: string}) => {
                    const key = `${variables.workspaceId}-${variables.environmentId}-${variables.page}`;

                    if (!resultsByVariables.has(key)) {
                        const original = variables.environmentId === '2' && variables.workspaceId === '7';

                        resultsByVariables.set(key, {
                            data: {
                                workspaceExternalInvocationLogs: {
                                    content: [
                                        {id: key, invocationName: `${original ? 'OLD' : 'NEW'}_PAGE_${variables.page}`},
                                    ],
                                    number: variables.page,
                                    size: 1,
                                    totalElements: 2,
                                    totalPages: 2,
                                },
                            },
                            isLoading: false,
                        });
                    }

                    return resultsByVariables.get(key);
                }
            );

            renderTab();

            await userEvent.click(screen.getByRole('button', {name: 'Load more'}));

            expect(screen.getByText('OLD_PAGE_0')).toBeInTheDocument();
            expect(screen.getByText('OLD_PAGE_1')).toBeInTheDocument();

            act(() => {
                switchScope();
            });

            expect(screen.queryByText('OLD_PAGE_0')).not.toBeInTheDocument();
            expect(screen.queryByText('OLD_PAGE_1')).not.toBeInTheDocument();
            expect(screen.getByText('NEW_PAGE_0')).toBeInTheDocument();
            expect(screen.queryByText('NEW_PAGE_1')).not.toBeInTheDocument();
            expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
                {appId: 1, environmentId, page: 0, type: 'AUTOMATION', workspaceId},
                {enabled: true}
            );
        }
    );
});
