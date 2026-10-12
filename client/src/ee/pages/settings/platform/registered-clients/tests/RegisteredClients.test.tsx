import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import RegisteredClients from '../RegisteredClients';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    registeredClient: {
        authorizationGrantTypes: ['authorization_code'],
        clientId: 'acme-client',
        clientIdIssuedAt: 0,
        clientName: 'Acme Client',
        deletable: true,
        grantCount: 2,
        id: 'client-1',
        redirectUris: ['https://client.example.com/callback'],
        scopes: ['mcp:automation'],
    },
    revokeAllMutate: vi.fn(),
}));

interface MutationOptionsI {
    onSuccess?: () => void;
}

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteRegisteredClientMutation: (options?: MutationOptionsI) => ({
        mutate: (variables: unknown) => {
            hoisted.deleteMutate(variables);

            options?.onSuccess?.();
        },
    }),
    useRegisteredClientsQuery: () => ({
        data: {registeredClients: [hoisted.registeredClient]},
        error: null,
        isLoading: false,
    }),
    useRevokeRegisteredClientGrantsMutation: (options?: MutationOptionsI) => ({
        isPending: false,
        mutate: (variables: unknown) => {
            hoisted.revokeAllMutate(variables);

            options?.onSuccess?.();
        },
    }),
}));

vi.mock('../components/RegisteredClientGrantsTable', () => ({
    default: ({registeredClientId}: {registeredClientId: string}) => <div>Grants for {registeredClientId}</div>,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

vi.mock('@/components/PageLoader', () => ({
    default: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/layout/Header', () => ({
    default: () => <div />,
}));

describe('RegisteredClients', () => {
    beforeEach(() => {
        hoisted.deleteMutate.mockClear();
        hoisted.invalidateQueries.mockClear();
        hoisted.revokeAllMutate.mockClear();
        hoisted.registeredClient.deletable = true;
    });

    it('lists the registered clients', () => {
        render(<RegisteredClients />);

        expect(screen.getByText('Acme Client')).toBeInTheDocument();
        expect(screen.getByText('acme-client')).toBeInTheDocument();
        expect(screen.getByText('mcp:automation')).toBeInTheDocument();
    });

    it('deletes a client after confirmation', () => {
        render(<RegisteredClients />);

        fireEvent.click(screen.getByLabelText('Delete client'));

        expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: 'client-1'});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['registeredClients']});
    });

    it('does not delete a client when the confirmation is cancelled', () => {
        render(<RegisteredClients />);

        fireEvent.click(screen.getByLabelText('Delete client'));
        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(hoisted.deleteMutate).not.toHaveBeenCalled();
        expect(screen.queryByText('Are you absolutely sure?')).not.toBeInTheDocument();
    });

    it('shows the grant count and expands the grants table', () => {
        render(<RegisteredClients />);

        expect(screen.getByText('2 user(s)')).toBeInTheDocument();
        expect(screen.queryByText('Grants for client-1')).not.toBeInTheDocument();

        fireEvent.click(screen.getByLabelText('Show grants'));

        expect(screen.getByText('Grants for client-1')).toBeInTheDocument();
    });

    it('offers revoke all grants instead of delete for a non-deletable client', () => {
        hoisted.registeredClient.deletable = false;

        render(<RegisteredClients />);

        expect(screen.queryByLabelText('Delete client')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Revoke all grants')).toBeInTheDocument();
    });

    it('revokes all grants of a client after confirmation', () => {
        hoisted.registeredClient.deletable = false;

        render(<RegisteredClients />);

        fireEvent.click(screen.getByLabelText('Revoke all grants'));
        fireEvent.click(screen.getByRole('button', {name: 'Revoke'}));

        expect(hoisted.revokeAllMutate).toHaveBeenCalledWith({registeredClientId: 'client-1'});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['registeredClientGrants']});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['registeredClients']});
    });

    it('does not revoke the grants when the confirmation is cancelled', () => {
        hoisted.registeredClient.deletable = false;

        render(<RegisteredClients />);

        fireEvent.click(screen.getByLabelText('Revoke all grants'));
        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(hoisted.revokeAllMutate).not.toHaveBeenCalled();
    });
});
