import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
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

interface MockExternalInvocationsTablePropsI {
    externalInvocationLogs: Array<{id: string; invocationName: string; userName?: string | null}>;
    showUser?: boolean;
}

vi.mock('@/pages/automation/external-invocations/components/ExternalInvocationsTable', () => ({
    default: ({externalInvocationLogs, showUser}: MockExternalInvocationsTablePropsI) => (
        <ul data-show-user={String(Boolean(showUser))}>
            {externalInvocationLogs.map((externalInvocationLog) => (
                <li key={externalInvocationLog.id}>
                    {externalInvocationLog.invocationName}

                    {showUser && <span>{externalInvocationLog.userName || '-'}</span>}
                </li>
            ))}
        </ul>
    ),
}));

const developmentInstance = {environmentId: '2', id: '11', workspaceId: '7'};

const renderTab = (appInstance: {environmentId: string; id: string; workspaceId: string} = developmentInstance) =>
    render(
        <MemoryRouter>
            <AppActivityTab appId="1" appInstance={appInstance as never} />
        </MemoryRouter>
    );

describe('AppActivityTab', () => {
    beforeEach(() => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReset();
        environmentStore.setState({currentEnvironmentId: 0});
        useWorkspaceStore.setState({currentWorkspaceId: 9});
    });

    it('says the activity could not be loaded instead of showing the empty state when the query fails', () => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue({
            data: undefined,
            error: new Error('Forbidden'),
            isLoading: false,
        });

        renderTab();

        expect(screen.getByText('The activity of this app could not be loaded.')).toBeInTheDocument();
        expect(screen.queryByText('No invocations recorded for this app yet.')).not.toBeInTheDocument();
    });

    it('queries the invocations of the app in the workspace and environment of the instance', () => {
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

        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenCalledWith({
            appId: 1,
            appInstanceId: '11',
            environmentId: '2',
            page: 0,
            type: 'AUTOMATION',
            workspaceId: '7',
        });
        expect(screen.getByText('SALES_SLACK__SEND_MESSAGE')).toBeInTheDocument();
        expect(screen.getByText('SALES_SLACK__LIST_CHANNELS')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Load more'})).not.toBeInTheDocument();
    });

    it('says that it shows the selected instance', () => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue({
            data: {
                workspaceExternalInvocationLogs: {content: [], number: 0, size: 20, totalElements: 0, totalPages: 0},
            },
            isLoading: false,
        });

        renderTab();

        expect(
            screen.getByText('The invocations of the selected instance of this app, in the Production environment.')
        ).toBeInTheDocument();
    });

    it('shows the end user of each invocation and a dash for calls made with the app key alone', () => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue({
            data: {
                workspaceExternalInvocationLogs: {
                    content: [
                        {id: '1', invocationName: 'SALES_SLACK__SEND_MESSAGE', userName: 'Ada Lovelace'},
                        {id: '2', invocationName: 'SALES_SLACK__LIST_CHANNELS', userName: null},
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

        expect(screen.getByRole('list')).toHaveAttribute('data-show-user', 'true');
        expect(screen.getByText('Ada Lovelace')).toBeInTheDocument();
        expect(screen.getByText('-')).toBeInTheDocument();
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
                <AppActivityTab appId="abc" appInstance={developmentInstance as never} />
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

        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith({
            appId: 1,
            appInstanceId: '11',
            environmentId: '2',
            page: 1,
            type: 'AUTOMATION',
            workspaceId: '7',
        });
    });

    it.each([
        ['instance of another environment', {environmentId: '1', id: '12', workspaceId: '7'}, '12', '1', '7'],
        ['instance of another workspace', {environmentId: '2', id: '13', workspaceId: '8'}, '13', '2', '8'],
        [
            'other instance of the same workspace and environment',
            {environmentId: '2', id: '14', workspaceId: '7'},
            '14',
            '2',
            '7',
        ],
    ])(
        'drops the loaded pages and returns to page 0 when the %s is chosen',
        async (_scope, otherInstance, appInstanceId, environmentId, workspaceId) => {
            const resultsByVariables = new Map<string, object>();

            hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockImplementation(
                (variables: {appInstanceId: string; environmentId: string; page: number; workspaceId: string}) => {
                    const key = `${variables.workspaceId}-${variables.environmentId}-${variables.appInstanceId}-${variables.page}`;

                    if (!resultsByVariables.has(key)) {
                        const original = variables.appInstanceId === '11';

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

            const {rerender} = renderTab();

            await userEvent.click(screen.getByRole('button', {name: 'Load more'}));

            expect(screen.getByText('OLD_PAGE_0')).toBeInTheDocument();
            expect(screen.getByText('OLD_PAGE_1')).toBeInTheDocument();

            rerender(
                <MemoryRouter>
                    <AppActivityTab appId="1" appInstance={otherInstance as never} />
                </MemoryRouter>
            );

            expect(screen.queryByText('OLD_PAGE_0')).not.toBeInTheDocument();
            expect(screen.queryByText('OLD_PAGE_1')).not.toBeInTheDocument();
            expect(screen.getByText('NEW_PAGE_0')).toBeInTheDocument();
            expect(screen.queryByText('NEW_PAGE_1')).not.toBeInTheDocument();
            expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith({
                appId: 1,
                appInstanceId,
                environmentId,
                page: 0,
                type: 'AUTOMATION',
                workspaceId,
            });
        }
    );
});
