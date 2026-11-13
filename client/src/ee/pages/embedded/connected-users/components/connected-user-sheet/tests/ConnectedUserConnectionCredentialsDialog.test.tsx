import ConnectedUserConnectionCredentialsDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserConnectionCredentialsDialog';
import {Connection, ConnectionApi} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectionKeys} from '@/ee/shared/queries/embedded/connections.queries';
import {QueryClient, QueryClientProvider, QueryKey, UseMutationResult} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

interface ConnectionDialogMockPropsI {
    componentDefinitions: Array<{name: string}>;
    connection: Connection;
    connectionsQueryKey: QueryKey;
    onClose: () => void;
    startInCredentialsMode?: boolean;
    useUpdateConnectionCredentialsMutation: (mutationProps: object) => UseMutationResult<void, Error, Connection>;
}

const hoisted = vi.hoisted(() => ({
    componentDefinitionsQuery: vi.fn(),
    connectionDialogProps: undefined as ConnectionDialogMockPropsI | undefined,
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: hoisted.componentDefinitionsQuery,
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: (props: ConnectionDialogMockPropsI) => {
        hoisted.connectionDialogProps = props;

        const credentialsMutation = props.useUpdateConnectionCredentialsMutation({});

        return (
            <button
                onClick={() =>
                    credentialsMutation.mutate({
                        ...props.connection,
                        parameters: {token: 'new-secret'},
                    } as Connection)
                }
                type="button"
            >
                Update credentials
            </button>
        );
    },
}));

const connection = {
    componentName: 'airtable',
    credentialStatus: 'VALID',
    id: 1051,
    name: 'Airtable',
    origin: 'PROVISIONED',
    version: 4,
} as Connection;

const renderDialog = (onClose = vi.fn()) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <ConnectedUserConnectionCredentialsDialog connectedUserId={7} connection={connection} onClose={onClose} />
        </QueryClientProvider>
    );

describe('ConnectedUserConnectionCredentialsDialog', () => {
    beforeEach(() => {
        hoisted.connectionDialogProps = undefined;

        hoisted.componentDefinitionsQuery.mockReset();
        hoisted.componentDefinitionsQuery.mockReturnValue({data: [{name: 'airtable'}]});

        vi.restoreAllMocks();
    });

    it("opens the connection's credentials for editing", () => {
        const onClose = vi.fn();

        renderDialog(onClose);

        expect(hoisted.connectionDialogProps?.connection).toBe(connection);
        expect(hoisted.connectionDialogProps?.startInCredentialsMode).toBe(true);
        expect(hoisted.connectionDialogProps?.componentDefinitions).toEqual([{name: 'airtable'}]);
        expect(hoisted.connectionDialogProps?.connectionsQueryKey).toEqual(
            ConnectionKeys.allConnectedUserConnections(7)
        );
        expect(hoisted.connectionDialogProps?.onClose).toBe(onClose);
    });

    it('waits for the component definitions before opening', () => {
        hoisted.componentDefinitionsQuery.mockReturnValue({data: undefined});

        renderDialog();

        expect(hoisted.connectionDialogProps).toBeUndefined();
    });

    it("replaces the credentials through the connected user's connection", async () => {
        const updateConnectedUserConnectionCredentials = vi
            .spyOn(ConnectionApi.prototype, 'updateConnectedUserConnectionCredentials')
            .mockResolvedValue();

        renderDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Update credentials'}));

        await waitFor(() => {
            expect(updateConnectedUserConnectionCredentials).toHaveBeenCalledWith({
                connectedUserId: 7,
                connectionId: 1051,
                updateConnectionCredentialsRequest: {parameters: {token: 'new-secret'}, version: 4},
            });
        });
    });
});
