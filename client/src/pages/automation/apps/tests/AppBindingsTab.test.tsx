import {render, screen, stubMutation, within} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppBindingsTab from '../components/AppBindingsTab';

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppBindableConnectionsQuery: vi.fn(() => ({data: {appBindableConnections: []}})),
    useAppBindingsQuery: vi.fn(() => ({
        data: {
            appBindings: [
                {
                    actionNames: ['sendMessage'],
                    appId: '3',
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
                    appId: '3',
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

const {useAppComponentOperationsQuery, useBindAppConnectionMutation, useUnbindAppConnectionMutation} =
    await import('@/shared/middleware/graphql');

stubMutation(vi.mocked(useBindAppConnectionMutation));
stubMutation(vi.mocked(useUnbindAppConnectionMutation));

describe('AppBindingsTab', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('marks a binding whose component cannot be resolved and keeps its operations read-only', () => {
        render(<AppBindingsTab app={{id: '3'} as never} canManage />);

        const legacyRow = screen.getByRole('row', {name: /legacy_crm/});

        expect(within(legacyRow).getByText('Unavailable')).toBeInTheDocument();
        expect(within(legacyRow).getByText('2')).toBeInTheDocument();
        expect(within(legacyRow).getByText('1')).toBeInTheDocument();

        const slackRow = screen.getByRole('row', {name: /sales_slack/});

        expect(within(slackRow).queryByText('Unavailable')).not.toBeInTheDocument();
    });

    it('never loads component operations for a binding', () => {
        render(<AppBindingsTab app={{id: '3'} as never} canManage />);

        expect(vi.mocked(useAppComponentOperationsQuery)).not.toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({enabled: true})
        );
    });
});
