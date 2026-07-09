import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import RegisteredClients from './RegisteredClients';

const deleteMutate = vi.fn();
const invalidateQueries = vi.fn();

const registeredClient = {
    authorizationGrantTypes: ['authorization_code'],
    clientId: 'acme-client',
    clientIdIssuedAt: 0,
    clientName: 'Acme Client',
    id: 'client-1',
    redirectUris: ['https://client.example.com/callback'],
    scopes: ['mcp:automation'],
};

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteRegisteredClientMutation: () => ({mutate: deleteMutate}),
    useRegisteredClientsQuery: () => ({
        data: {registeredClients: [registeredClient]},
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries}),
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
        deleteMutate.mockClear();
        invalidateQueries.mockClear();
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

        expect(deleteMutate).toHaveBeenCalledWith({id: 'client-1'});
    });
});
