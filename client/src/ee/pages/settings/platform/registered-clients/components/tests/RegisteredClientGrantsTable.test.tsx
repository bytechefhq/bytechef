import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import RegisteredClientGrantsTable from '../RegisteredClientGrantsTable';

const ALICE_GRANT = {
    authorizedAt: 1759276800000,
    expiresAt: 1761955200000,
    lastIssuedAt: 1759363200000,
    mcpServers: ['Management', 'Sales Automation'],
    principalName: 'alice@example.com',
    scopes: ['mcp:automation', 'mcp:management'],
};

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    queryResult: {} as {data?: unknown; error?: unknown; isLoading: boolean},
    revokeMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useRegisteredClientGrantsQuery: () => hoisted.queryResult,
    useRevokeRegisteredClientGrantMutation: (options?: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: unknown) => {
            hoisted.revokeMutate(variables);

            options?.onSuccess?.();
        },
    }),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

describe('RegisteredClientGrantsTable', () => {
    beforeEach(() => {
        hoisted.queryResult = {data: {registeredClientGrants: [ALICE_GRANT]}, error: null, isLoading: false};
        hoisted.invalidateQueries.mockReset();
        hoisted.revokeMutate.mockReset();
    });

    it('lists each user with the MCP servers the grant reaches', () => {
        render(<RegisteredClientGrantsTable registeredClientId="client-1" />);

        expect(screen.getByText('alice@example.com')).toBeInTheDocument();
        expect(screen.getByText('Management, Sales Automation')).toBeInTheDocument();
    });

    it('revokes a user grant after confirmation', async () => {
        render(<RegisteredClientGrantsTable registeredClientId="client-1" />);

        await userEvent.click(screen.getByRole('button', {name: 'Revoke alice@example.com'}));
        await userEvent.click(screen.getByRole('button', {name: 'Continue'}));

        expect(hoisted.revokeMutate).toHaveBeenCalledWith({
            principalName: 'alice@example.com',
            registeredClientId: 'client-1',
        });
    });

    it('refreshes the grants and the client list after a grant is revoked', async () => {
        render(<RegisteredClientGrantsTable registeredClientId="client-1" />);

        await userEvent.click(screen.getByRole('button', {name: 'Revoke alice@example.com'}));
        await userEvent.click(screen.getByRole('button', {name: 'Continue'}));

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['registeredClientGrants']});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['registeredClients']});
    });

    it('shows an error instead of the empty state when the grants cannot be loaded', () => {
        hoisted.queryResult = {data: undefined, error: new Error('Access denied'), isLoading: false};

        render(<RegisteredClientGrantsTable registeredClientId="client-1" />);

        expect(screen.getByRole('alert')).toHaveTextContent(
            'The users who authorized this client could not be loaded.'
        );
        expect(screen.queryByText('No user has authorized this client.')).not.toBeInTheDocument();
    });

    it('shows the empty state when no user has authorized the client', () => {
        hoisted.queryResult = {data: {registeredClientGrants: []}, error: null, isLoading: false};

        render(<RegisteredClientGrantsTable registeredClientId="client-1" />);

        expect(screen.getByText('No user has authorized this client.')).toBeInTheDocument();
    });

    it('shows a dash for an authorization time the server did not record', () => {
        hoisted.queryResult = {
            data: {registeredClientGrants: [{...ALICE_GRANT, authorizedAt: null, lastIssuedAt: null}]},
            error: null,
            isLoading: false,
        };

        render(<RegisteredClientGrantsTable registeredClientId="client-1" />);

        expect(screen.getAllByText('—')).toHaveLength(2);
    });
});
