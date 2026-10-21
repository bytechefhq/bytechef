import Button from '@/components/Button/Button';
import {
    mockScrollIntoView,
    render,
    screen,
    stubMutation,
    userEvent,
    waitFor,
    windowResizeObserver,
} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppBindingDialog from '../components/AppBindingDialog';

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppBindableConnectionsQuery: vi.fn(() => ({
        data: {
            appBindableConnections: [
                {componentName: 'slack', componentVersion: 4, connectionVersion: 1, id: '11', name: 'Sales Slack'},
            ],
        },
    })),
    useAppComponentOperationsQuery: vi.fn(() => ({
        data: {appComponentOperations: {actionNames: ['sendMessage', 'sendDirectMessage'], toolNames: ['sendMessage']}},
    })),
    useBindAppConnectionMutation: vi.fn(),
}));

const {useAppComponentOperationsQuery, useBindAppConnectionMutation} = await import('@/shared/middleware/graphql');

const bindMutation = stubMutation(vi.mocked(useBindAppConnectionMutation));

describe('AppBindingDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        windowResizeObserver();
        mockScrollIntoView();
    });

    it('rejects an invalid binding key', async () => {
        render(<AppBindingDialog app={{id: '3'} as never} triggerNode={<Button label="Bind Connection" />} />);

        await userEvent.click(screen.getByRole('button', {name: 'Bind Connection'}));
        await userEvent.type(screen.getByLabelText('Binding key'), 'Sales Slack');
        await userEvent.click(screen.getByRole('button', {name: 'Bind'}));

        expect(await screen.findByText(/Binding key must start with a lowercase letter/)).toBeInTheDocument();
        expect(bindMutation.mutate).not.toHaveBeenCalled();
    });

    it('offers the operations of the latest component version, not the connection version', async () => {
        render(<AppBindingDialog app={{id: '3'} as never} triggerNode={<Button label="Bind Connection" />} />);

        await userEvent.click(screen.getByRole('button', {name: 'Bind Connection'}));
        await userEvent.click(screen.getByRole('combobox', {name: 'Connection'}));
        await userEvent.click(await screen.findByText('Sales Slack (slack)'));

        await waitFor(() => {
            expect(vi.mocked(useAppComponentOperationsQuery)).toHaveBeenLastCalledWith(
                {componentName: 'slack', componentVersion: 4},
                {enabled: true}
            );
        });

        expect(vi.mocked(useAppComponentOperationsQuery)).not.toHaveBeenCalledWith(
            {componentName: 'slack', componentVersion: 1},
            expect.anything()
        );
    });

    it('binds a connection with the selected actions and tools', async () => {
        render(<AppBindingDialog app={{id: '3'} as never} triggerNode={<Button label="Bind Connection" />} />);

        await userEvent.click(screen.getByRole('button', {name: 'Bind Connection'}));
        await userEvent.type(screen.getByLabelText('Binding key'), 'sales_slack');
        await userEvent.click(screen.getByRole('combobox', {name: 'Connection'}));
        await userEvent.click(await screen.findByText('Sales Slack (slack)'));
        await userEvent.click(await screen.findByLabelText('sendDirectMessage'));
        await userEvent.click(screen.getAllByLabelText('sendMessage')[1]);
        await userEvent.click(screen.getByRole('button', {name: 'Bind'}));

        await waitFor(() => {
            expect(bindMutation.mutate).toHaveBeenCalledWith({
                appId: '3',
                input: {
                    actionNames: ['sendDirectMessage'],
                    bindingKey: 'sales_slack',
                    connectionId: '11',
                    proxyAllowedMethods: [],
                    proxyEnabled: false,
                    toolNames: ['sendMessage'],
                },
            });
        });
    });
});
