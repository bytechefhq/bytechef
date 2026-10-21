import {
    mockScrollIntoView,
    render,
    screen,
    stubMutation,
    userEvent,
    waitFor,
    windowResizeObserver,
    within,
} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AppDeploymentSummary, {appDeploymentSummaryRefetchInterval} from '../components/AppDeploymentSummary';
import AppDeploymentsTab from '../components/AppDeploymentsTab';
import {appDeploymentsRefetchInterval} from '../hooks/useAppDeployments';

const hoisted = vi.hoisted(() => ({
    useAppDeploymentsQueryMock: vi.fn(),
    useAppInstanceDeploymentSummaryQueryMock: vi.fn(),
    useAppInstanceVersionsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppDeploymentsQuery: hoisted.useAppDeploymentsQueryMock,
    useAppInstanceDeploymentSummaryQuery: hoisted.useAppInstanceDeploymentSummaryQueryMock,
    useAppInstanceVersionsQuery: hoisted.useAppInstanceVersionsQueryMock,
    useCancelAppDeploymentMutation: vi.fn(),
    useDeployAppInstanceMutation: vi.fn(),
    useRollbackAppInstanceMutation: vi.fn(),
}));

const {useCancelAppDeploymentMutation, useDeployAppInstanceMutation, useRollbackAppInstanceMutation} =
    await import('@/shared/middleware/graphql');

const cancelMutation = stubMutation(vi.mocked(useCancelAppDeploymentMutation));
const deployMutation = stubMutation(vi.mocked(useDeployAppInstanceMutation));
const rollbackMutation = stubMutation(vi.mocked(useRollbackAppInstanceMutation));

const appVersions = [
    {appId: '5', createdDate: '2026-11-12T10:00:00Z', gitCommitSha: 'abcdef1234567', id: '3', name: 'v3'},
    {appId: '5', createdDate: '2026-11-11T10:00:00Z', gitCommitSha: null, id: '2', name: 'v2'},
];

const deployableProps = {
    appInstanceId: '21',
    canDeploy: true,
    deploymentConnectionId: '31',
    deploymentProviderAvailable: true,
};

const deployment = (overrides: Record<string, unknown>) => ({
    appInstanceId: '21',
    appVersion: {id: '1', name: 'v1'},
    createdBy: 'ci-bot',
    createdDate: '2026-11-10T10:00:00Z',
    current: false,
    errorMessage: null,
    finishedDate: '2026-11-10T10:05:00Z',
    id: '1',
    provider: 'EXTERNAL',
    status: 'READY',
    url: null,
    ...overrides,
});

const mockDeployments = (appDeployments: Array<Record<string, unknown>>) => {
    hoisted.useAppDeploymentsQueryMock.mockReturnValue({data: {appDeployments}, error: null, isLoading: false});
};

describe('AppDeploymentsTab', () => {
    beforeEach(() => {
        hoisted.useAppDeploymentsQueryMock.mockReset();
        hoisted.useAppInstanceVersionsQueryMock.mockReset();
        hoisted.useAppInstanceVersionsQueryMock.mockReturnValue({
            data: {appInstanceVersions: appVersions},
            isLoading: false,
        });
        cancelMutation.mutate.mockReset();
        deployMutation.mutate.mockReset();
        rollbackMutation.mutate.mockReset();
        windowResizeObserver();
        mockScrollIntoView();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('polls the list every 5 seconds only while a deployment is queued or building', () => {
        mockDeployments([deployment({status: 'BUILDING'})]);

        render(<AppDeploymentsTab appInstanceId="21" />);

        expect(hoisted.useAppDeploymentsQueryMock).toHaveBeenCalledWith(
            {appInstanceId: '21', limit: 50},
            expect.objectContaining({refetchInterval: appDeploymentsRefetchInterval})
        );

        const queryWith = (statuses: string[]) => ({
            state: {
                data: {
                    appDeployments: statuses.map((status, index) => deployment({id: String(index), status})) as never,
                },
            },
        });

        expect(appDeploymentsRefetchInterval(queryWith(['READY', 'QUEUED']))).toBe(5000);
        expect(appDeploymentsRefetchInterval(queryWith(['BUILDING']))).toBe(5000);
        expect(appDeploymentsRefetchInterval(queryWith(['READY', 'FAILED', 'CANCELED']))).toBe(false);
        expect(appDeploymentsRefetchInterval(queryWith([]))).toBe(false);
        expect(appDeploymentsRefetchInterval({state: {}})).toBe(false);
    });

    it('refetches the instance and the summary once the last deployment in progress finishes', async () => {
        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');

        mockDeployments([deployment({status: 'BUILDING'})]);

        const {rerender} = render(<AppDeploymentsTab appInstanceId="21" />);

        expect(invalidateQueriesSpy).not.toHaveBeenCalled();

        mockDeployments([deployment({status: 'READY'})]);

        rerender(<AppDeploymentsTab appInstanceId="21" />);

        await waitFor(() => {
            expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['appInstance']});
        });

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['appInstanceDeploymentSummary']});
    });

    it('hides Deploy, Roll back and Cancel without APP_DEPLOY', () => {
        mockDeployments([deployment({id: '2', status: 'READY'}), deployment({id: '1', status: 'QUEUED'})]);

        render(<AppDeploymentsTab {...deployableProps} canDeploy={false} />);

        expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /Roll back/})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /Cancel/})).not.toBeInTheDocument();
        expect(hoisted.useAppInstanceVersionsQueryMock).toHaveBeenCalledWith(
            {appInstanceId: '21', limit: 50},
            expect.objectContaining({enabled: false})
        );
    });

    it.each([
        [
            'no provider is available',
            {deploymentProviderAvailable: false},
            'No deployment provider is available for the hosting target of this instance.',
        ],
        [
            'no deployment connection is configured',
            {deploymentConnectionId: null},
            'Configure a deployment connection in the deployment settings of this instance first.',
        ],
    ])('disables Deploy when %s', (_reason, overrides, explanation) => {
        mockDeployments([]);

        render(<AppDeploymentsTab {...deployableProps} {...overrides} />);

        expect(screen.getByRole('button', {name: 'Deploy'})).toBeDisabled();
        expect(screen.getByText(explanation)).toBeInTheDocument();
    });

    it('disables Deploy while a deployment is queued or building', () => {
        mockDeployments([deployment({status: 'QUEUED'})]);

        render(<AppDeploymentsTab {...deployableProps} />);

        expect(screen.getByRole('button', {name: 'Deploy'})).toBeDisabled();
        expect(screen.getByText(/A deployment is in progress/)).toBeInTheDocument();
    });

    it('disables Deploy and points at version create when the app has no versions', () => {
        mockDeployments([]);
        hoisted.useAppInstanceVersionsQueryMock.mockReturnValue({data: {appInstanceVersions: []}, isLoading: false});

        render(<AppDeploymentsTab {...deployableProps} />);

        expect(screen.getByRole('button', {name: 'Deploy'})).toBeDisabled();
        expect(screen.getByText(/bytechef automation app version create/)).toBeInTheDocument();
    });

    it('deploys the version chosen in the dialog and refetches the deployment views', async () => {
        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');

        mockDeployments([]);

        render(<AppDeploymentsTab {...deployableProps} />);

        expect(hoisted.useAppInstanceVersionsQueryMock).toHaveBeenCalledWith(
            {appInstanceId: '21', limit: 50},
            expect.objectContaining({enabled: true})
        );

        await userEvent.click(screen.getByRole('button', {name: 'Deploy'}));

        const dialog = await screen.findByRole('dialog');

        expect(within(dialog).getByRole('combobox', {name: 'Version'})).toHaveTextContent('v3 (abcdef1)');

        await userEvent.click(within(dialog).getByRole('combobox', {name: 'Version'}));
        await userEvent.click(await screen.findByText('v2'));
        await userEvent.click(within(dialog).getByRole('button', {name: 'Deploy'}));

        expect(deployMutation.mutate).toHaveBeenCalledWith({appInstanceId: '21', appVersionId: '2'});

        deployMutation.triggerOnSuccess(undefined, {appInstanceId: '21', appVersionId: '2'});

        for (const queryKey of ['appDeployments', 'appInstance', 'appInstanceDeploymentSummary']) {
            expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: [queryKey]});
        }

        await waitFor(() => {
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });
    });

    it('offers Roll back only on ready deployments that are not current and rolls back to that version', async () => {
        mockDeployments([
            deployment({appVersion: {id: '3', name: 'v3'}, current: true, id: '3', status: 'READY'}),
            deployment({appVersion: {id: '2', name: 'v2'}, id: '2', status: 'READY'}),
            deployment({appVersion: {id: '1', name: 'v1'}, id: '1', status: 'FAILED'}),
        ]);

        render(<AppDeploymentsTab {...deployableProps} />);

        const rollbackButtons = screen.getAllByRole('button', {name: /^Roll back/});

        expect(rollbackButtons).toHaveLength(1);
        expect(rollbackButtons[0]).toHaveAccessibleName('Roll back to v2');

        await userEvent.click(rollbackButtons[0]);

        const confirmation = await screen.findByRole('alertdialog');

        expect(rollbackMutation.mutate).not.toHaveBeenCalled();

        await userEvent.click(within(confirmation).getByRole('button', {name: 'Roll back'}));

        expect(rollbackMutation.mutate).toHaveBeenCalledWith({appInstanceId: '21', appVersionId: '2'});
    });

    it('offers Cancel only on queued or building deployments and cancels after confirmation', async () => {
        mockDeployments([
            deployment({appVersion: {id: '3', name: 'v3'}, id: '13', status: 'BUILDING'}),
            deployment({appVersion: {id: '2', name: 'v2'}, id: '12', status: 'CANCELED'}),
            deployment({appVersion: {id: '1', name: 'v1'}, current: true, id: '11', status: 'READY'}),
        ]);

        render(<AppDeploymentsTab {...deployableProps} />);

        const cancelButtons = screen.getAllByRole('button', {name: /^Cancel the deployment/});

        expect(cancelButtons).toHaveLength(1);
        expect(cancelButtons[0]).toHaveAccessibleName('Cancel the deployment of v3');

        await userEvent.click(cancelButtons[0]);

        const confirmation = await screen.findByRole('alertdialog');

        expect(cancelMutation.mutate).not.toHaveBeenCalled();

        await userEvent.click(within(confirmation).getByRole('button', {name: 'Cancel deployment'}));

        expect(cancelMutation.mutate).toHaveBeenCalledWith({appDeploymentId: '13', appInstanceId: '21'});
    });

    it('renders a provider error message that looks like markup as plain text', () => {
        const errorMessage = '<img src=x onerror="alert(1)"><a href="https://evil.example.com">click</a>';

        mockDeployments([deployment({errorMessage, provider: 'DEPLOY_HOOK', status: 'FAILED'})]);

        const {container} = render(<AppDeploymentsTab {...deployableProps} />);

        expect(screen.getByText(errorMessage)).toBeInTheDocument();
        expect(container.querySelector('img')).toBeNull();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
        expect(screen.getByText('Deploy hook')).toBeInTheDocument();
    });

    it('lists the deployments newest first with version, status, who recorded it and the link', () => {
        mockDeployments([
            deployment({
                appVersion: {id: '2', name: 'v2'},
                createdDate: '2026-11-12T10:00:00Z',
                current: true,
                id: '2',
                provider: 'VERCEL',
                url: 'https://new.example.com',
            }),
            deployment({id: '1', url: 'https://old.example.com'}),
        ]);

        render(<AppDeploymentsTab appInstanceId="21" />);

        expect(hoisted.useAppDeploymentsQueryMock).toHaveBeenCalledWith(
            {appInstanceId: '21', limit: 50},
            expect.objectContaining({placeholderData: expect.anything()})
        );

        const rows = screen.getAllByRole('row').slice(1);

        expect(rows).toHaveLength(2);
        expect(within(rows[0]).getByText('v2')).toBeInTheDocument();
        expect(within(rows[0]).getByText('Ready')).toBeInTheDocument();
        expect(within(rows[0]).getByText('Vercel')).toBeInTheDocument();
        expect(within(rows[0]).getByText('ci-bot')).toBeInTheDocument();
        expect(within(rows[0]).getByRole('link', {name: 'https://new.example.com'})).toHaveAttribute(
            'href',
            'https://new.example.com'
        );
        expect(within(rows[1]).getByText('v1')).toBeInTheDocument();
    });

    it('opens the link in a new tab without an opener', () => {
        mockDeployments([deployment({url: 'http://plain.example.com'})]);

        render(<AppDeploymentsTab appInstanceId="21" />);

        const link = screen.getByRole('link', {name: 'http://plain.example.com'});

        expect(link).toHaveAttribute('target', '_blank');
        expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('renders a url that is not http or https as plain text', () => {
        mockDeployments([deployment({url: 'javascript:alert(1)'})]);

        render(<AppDeploymentsTab appInstanceId="21" />);

        expect(screen.getByText('javascript:alert(1)')).toBeInTheDocument();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });

    it('shows the error message of a failed deployment', () => {
        mockDeployments([deployment({errorMessage: 'Build exited with code 1', status: 'FAILED'})]);

        render(<AppDeploymentsTab appInstanceId="21" />);

        expect(screen.getByText('Failed')).toBeInTheDocument();
        expect(screen.getByText('Build exited with code 1')).toBeInTheDocument();
    });

    it('explains how deployments are recorded when there are none', () => {
        mockDeployments([]);

        render(<AppDeploymentsTab appInstanceId="21" />);

        expect(screen.getByText(/bytechef automation app deployment record/)).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it.each(['HTTP://example.com', 'data:text/html,hello'])('renders %s as plain text without a link', (url) => {
        mockDeployments([deployment({url})]);

        render(<AppDeploymentsTab appInstanceId="21" />);

        expect(screen.getByText(url)).toBeInTheDocument();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });

    it('shows more rows by asking for the next limit and stops at the server maximum', async () => {
        const fullPage = (size: number) =>
            Array.from({length: size}, (_unused, index) => deployment({id: String(index + 1)}));

        hoisted.useAppDeploymentsQueryMock.mockImplementation(({limit}: {limit: number}) => ({
            data: {appDeployments: fullPage(limit)},
            error: null,
            isLoading: false,
        }));

        render(<AppDeploymentsTab appInstanceId="21" />);

        await userEvent.click(screen.getByRole('button', {name: 'Show more'}));

        expect(hoisted.useAppDeploymentsQueryMock).toHaveBeenLastCalledWith(
            {appInstanceId: '21', limit: 100},
            expect.anything()
        );

        await userEvent.click(screen.getByRole('button', {name: 'Show more'}));

        expect(hoisted.useAppDeploymentsQueryMock).toHaveBeenLastCalledWith(
            {appInstanceId: '21', limit: 200},
            expect.anything()
        );
        expect(screen.queryByRole('button', {name: 'Show more'})).not.toBeInTheDocument();
        expect(screen.getByText('Only the newest 200 deployments are shown.')).toBeInTheDocument();
    });

    it('offers no Show more when fewer rows than the limit come back', () => {
        mockDeployments([deployment({id: '1'})]);

        render(<AppDeploymentsTab appInstanceId="21" />);

        expect(screen.queryByRole('button', {name: 'Show more'})).not.toBeInTheDocument();
    });
});

describe('AppDeploymentSummary', () => {
    beforeEach(() => {
        hoisted.useAppInstanceDeploymentSummaryQueryMock.mockReset();
    });

    const mockSummary = (currentDeployment: unknown, deployments: Array<Record<string, unknown>>) => {
        hoisted.useAppInstanceDeploymentSummaryQueryMock.mockReturnValue({
            data: {appInstance: {currentDeployment, deployments, id: '21'}},
            isLoading: false,
        });
    };

    it('shows the version of the current deployment and the status of the latest attempt', () => {
        mockSummary({appVersion: {id: '1', name: 'v1'}, id: '1', status: 'READY'}, [
            deployment({appVersion: {id: '2', name: 'v2'}, id: '2', status: 'FAILED'}),
        ]);

        render(<AppDeploymentSummary appInstanceId="21" />);

        expect(hoisted.useAppInstanceDeploymentSummaryQueryMock).toHaveBeenCalledWith(
            {id: '21'},
            {refetchInterval: appDeploymentSummaryRefetchInterval}
        );
        expect(screen.getByText('Current version')).toBeInTheDocument();
        expect(screen.getByText('v1')).toBeInTheDocument();
        expect(screen.getByText('Latest deployment')).toBeInTheDocument();
        expect(screen.getByText('Failed')).toBeInTheDocument();
        expect(screen.queryByText('v2')).not.toBeInTheDocument();
    });

    it('says the status is unavailable when the query failed', () => {
        hoisted.useAppInstanceDeploymentSummaryQueryMock.mockReturnValue({
            data: undefined,
            error: new Error('boom'),
            isLoading: false,
        });

        render(<AppDeploymentSummary appInstanceId="21" />);

        expect(screen.getByText('Deployment status unavailable')).toBeInTheDocument();
        expect(screen.queryByText('No deployments yet')).not.toBeInTheDocument();
    });

    it('polls the summary every 5 seconds only while the latest deployment is in progress', () => {
        const queryWith = (status?: string) => ({
            state: {
                data: {
                    appInstance: {
                        currentDeployment: null,
                        deployments: status ? [deployment({status}) as never] : [],
                        id: '21',
                    },
                },
            },
        });

        expect(appDeploymentSummaryRefetchInterval(queryWith('QUEUED'))).toBe(5000);
        expect(appDeploymentSummaryRefetchInterval(queryWith('BUILDING'))).toBe(5000);
        expect(appDeploymentSummaryRefetchInterval(queryWith('READY'))).toBe(false);
        expect(appDeploymentSummaryRefetchInterval(queryWith())).toBe(false);
    });

    it('says there is no deployment yet', () => {
        mockSummary(null, []);

        render(<AppDeploymentSummary appInstanceId="21" />);

        expect(screen.getByText('No deployments yet')).toBeInTheDocument();
    });
});
