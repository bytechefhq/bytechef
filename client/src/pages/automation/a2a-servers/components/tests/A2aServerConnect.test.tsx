import {A2aServer} from '@/shared/middleware/graphql';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aServerConnect from '../A2aServerConnect';

const hoisted = vi.hoisted(() => ({
    copyToClipboard: vi.fn(),
}));

vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [null, hoisted.copyToClipboard],
}));

const a2aServer = {
    authenticationRequired: true,
    id: '7',
    name: 'Sales agent',
    secretKey: 'server-secret',
} as A2aServer;

const endpointUrl = `${window.location.origin}/api/automation/a2a/server-secret`;

describe('A2aServerConnect', () => {
    beforeEach(() => {
        hoisted.copyToClipboard.mockReset();
    });

    it('copies the agent card and endpoint URLs', async () => {
        const user = userEvent.setup();

        render(<A2aServerConnect a2aServer={a2aServer} />);

        await user.click(screen.getByRole('button', {name: 'Copy Agent Card URL'}));

        expect(hoisted.copyToClipboard).toHaveBeenCalledWith(`${endpointUrl}/.well-known/agent-card.json`);

        await user.click(screen.getByRole('button', {name: 'Copy Endpoint URL'}));

        expect(hoisted.copyToClipboard).toHaveBeenCalledWith(endpointUrl);
    });

    it('shows the Authorization header a client must send when authentication is required', async () => {
        render(<A2aServerConnect a2aServer={a2aServer} />);

        expect(screen.getByText('Authorization: Bearer YOUR_API_KEY')).toBeInTheDocument();
        expect(screen.getByText(/workspace API key from this server's workspace and environment/)).toBeInTheDocument();
        expect(screen.getByText(/X-ENVIRONMENT header must name this server's environment/)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Copy Authorization header'}));

        expect(hoisted.copyToClipboard).toHaveBeenCalledWith('Authorization: Bearer YOUR_API_KEY');
    });

    it('explains that the URLs work as is without authentication', () => {
        render(<A2aServerConnect a2aServer={{...a2aServer, authenticationRequired: false}} />);

        expect(screen.getByText(/does not require authentication/)).toBeInTheDocument();
        expect(screen.queryByText('Authorization: Bearer YOUR_API_KEY')).not.toBeInTheDocument();
    });
});
