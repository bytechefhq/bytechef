import ConnectedUserNewConnectionDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserNewConnectionDialog';
import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {QueryClient, QueryClientProvider, UseMutationResult} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    componentDefinitionRequests: vi.fn(),
    connectionDialogProps: vi.fn(),
    createConnectedUserConnection: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/embedded/configuration', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    ConnectionApi: class {
        createConnectedUserConnection = hoisted.createConnectedUserConnection;
    },
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [
            {name: 'slack', title: 'Slack', version: 2},
            {name: 'gmail', title: 'Gmail', version: 1},
        ],
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useGetComponentDefinitionQuery: (request: unknown, enabled?: boolean) => {
        hoisted.componentDefinitionRequests(request, enabled);

        return {data: {name: 'slack', title: 'Slack', version: 2}};
    },
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: (props: {
        onConnectionCreate: (connectionId: number) => void;
        useCreateConnectionMutation: (mutationProps: {
            onSuccess?: (result: number) => void;
        }) => UseMutationResult<number, Error, Connection>;
    }) => {
        hoisted.connectionDialogProps(props);

        const mutation = props.useCreateConnectionMutation({onSuccess: props.onConnectionCreate});

        return (
            <button
                onClick={() => mutation.mutate({componentName: 'slack', name: 'New Slack'} as Connection)}
                type="button"
            >
                Save connection
            </button>
        );
    },
}));

const renderDialog = (onConnectionCreate = vi.fn()) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <ConnectedUserNewConnectionDialog
                componentName="slack"
                connectedUserId={7}
                onClose={vi.fn()}
                onConnectionCreate={onConnectionCreate}
            />
        </QueryClientProvider>
    );

describe('ConnectedUserNewConnectionDialog', () => {
    beforeEach(() => {
        hoisted.componentDefinitionRequests.mockReset();
        hoisted.connectionDialogProps.mockReset();
        hoisted.createConnectedUserConnection.mockReset();

        hoisted.createConnectedUserConnection.mockResolvedValue(99);
    });

    it('opens the connection dialog for the component in its current version', () => {
        renderDialog();

        expect(hoisted.componentDefinitionRequests).toHaveBeenCalledWith(
            {componentName: 'slack', componentVersion: 2},
            true
        );
        expect(hoisted.connectionDialogProps).toHaveBeenCalledWith(
            expect.objectContaining({componentDefinition: {name: 'slack', title: 'Slack', version: 2}})
        );
    });

    it('creates the connection for the connected user and hands back its id', async () => {
        const onConnectionCreate = vi.fn();

        renderDialog(onConnectionCreate);

        await userEvent.click(screen.getByRole('button', {name: 'Save connection'}));

        await waitFor(() => expect(onConnectionCreate).toHaveBeenCalled());

        expect(onConnectionCreate.mock.calls[0][0]).toBe(99);

        expect(hoisted.createConnectedUserConnection).toHaveBeenCalledWith({
            connectedUserId: 7,
            connection: {componentName: 'slack', name: 'New Slack'},
        });
    });
});
