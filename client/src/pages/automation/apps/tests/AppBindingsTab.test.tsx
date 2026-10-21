import {render, screen, stubMutation, userEvent, within} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, onTestFinished, vi} from 'vitest';

import AppBindingsTab from '../components/AppBindingsTab';

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppBindableConnectionsQuery: vi.fn(() => ({data: {appBindableConnections: []}})),
    useAppBindingsQuery: vi.fn(() => ({
        data: {
            appBindings: [
                {
                    actionNames: ['sendMessage'],
                    appInstanceId: '3',
                    bindingKey: 'sales_slack',
                    componentName: 'slack',
                    componentVersion: 4,
                    connectionId: '11',
                    connectionName: 'Sales Slack',
                    connectionVersion: 1,
                    id: '21',
                    proxyAllowedMethods: [],
                    proxyEnabled: false,
                    toolNames: [],
                },
                {
                    actionNames: ['createRecord', 'updateRecord'],
                    appInstanceId: '3',
                    bindingKey: 'legacy_crm',
                    componentName: 'legacy-crm',
                    componentVersion: null,
                    connectionId: '12',
                    connectionName: 'Legacy CRM',
                    connectionVersion: 2,
                    id: '22',
                    proxyAllowedMethods: [],
                    proxyEnabled: false,
                    toolNames: ['searchRecords'],
                },
            ],
        },
        error: null,
        isLoading: false,
    })),
    useAppComponentOperationsQuery: vi.fn(() => ({data: undefined})),
    useBindAppConnectionMutation: vi.fn(),
    useUnbindAppConnectionMutation: vi.fn(),
}));

const {
    useAppBindingsQuery,
    useAppComponentOperationsQuery,
    useBindAppConnectionMutation,
    useUnbindAppConnectionMutation,
} = await import('@/shared/middleware/graphql');

stubMutation(vi.mocked(useBindAppConnectionMutation));
stubMutation(vi.mocked(useUnbindAppConnectionMutation));

describe('AppBindingsTab', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('marks a binding whose component cannot be resolved and keeps its operations read-only', () => {
        render(<AppBindingsTab appInstanceId="3" canManage />);

        const legacyRow = screen.getByRole('row', {name: /legacy_crm/});

        expect(within(legacyRow).getByText('Unavailable')).toBeInTheDocument();
        expect(within(legacyRow).getByText('2')).toBeInTheDocument();
        expect(within(legacyRow).getByText('1')).toBeInTheDocument();

        const slackRow = screen.getByRole('row', {name: /sales_slack/});

        expect(within(slackRow).queryByText('Unavailable')).not.toBeInTheDocument();
    });

    it('opens the binding pre-filled from Edit in the row menu', async () => {
        render(<AppBindingsTab appInstanceId="3" canManage />);

        await userEvent.click(screen.getByRole('button', {name: 'sales_slack actions'}));
        await userEvent.click(await screen.findByRole('menuitem', {name: 'Edit'}));

        expect(await screen.findByRole('heading', {name: 'Edit Binding'})).toBeInTheDocument();
        expect(screen.getByLabelText('Binding key')).toHaveValue('sales_slack');
    });

    it('offers Unbind in the row menu', async () => {
        render(<AppBindingsTab appInstanceId="3" canManage />);

        await userEvent.click(screen.getByRole('button', {name: 'legacy_crm actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Unbind'})).toBeInTheDocument();
    });

    it('shows a centered empty state with Bind Connection when nothing is bound', () => {
        vi.mocked(useAppBindingsQuery).mockReturnValue({
            data: {appBindings: []},
            error: null,
            isLoading: false,
        } as never);
        onTestFinished(() => {
            vi.mocked(useAppBindingsQuery).mockReset();
        });

        render(<AppBindingsTab appInstanceId="3" canManage />);

        expect(screen.getByRole('heading', {name: 'No connections bound'})).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Bind Connection'})).toHaveLength(1);
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('hides the row menu without APP_MANAGE', () => {
        render(<AppBindingsTab appInstanceId="3" canManage={false} />);

        expect(screen.queryByRole('button', {name: 'sales_slack actions'})).not.toBeInTheDocument();
    });

    it('never loads component operations for a binding', () => {
        render(<AppBindingsTab appInstanceId="3" canManage />);

        expect(vi.mocked(useAppComponentOperationsQuery)).not.toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({enabled: true})
        );
    });
});
