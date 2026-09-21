import {McpServer, PlatformType} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerDialog from '../McpServerDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useCreateMcpServerMutation: () => ({mutate: hoisted.createMutate}),
    useUpdateMcpServerMutation: () => ({mutate: hoisted.updateMutate}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => {
    const state = {currentEnvironmentId: 2};

    return {useEnvironmentStore: (selector: (currentState: typeof state) => unknown) => selector(state)};
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => {
    const state = {currentWorkspaceId: 1};

    return {useWorkspaceStore: (selector: (currentState: typeof state) => unknown) => selector(state)};
});

const renderDialog = (mcpServer?: McpServer) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <McpServerDialog
                mcpServer={mcpServer}
                onOpenChange={vi.fn()}
                open
                triggerNode={<button type="button">open</button>}
            />
        </QueryClientProvider>
    );

const getAuthenticationCheckbox = () => screen.getByRole('checkbox', {name: 'Require authentication'});

const getEnforceToolAuthorizationCheckbox = () => screen.getByRole('checkbox', {name: 'Enforce tool authorization'});

const existingMcpServer = {
    authenticationRequired: true,
    enabled: true,
    enforceToolAuthorization: true,
    id: '7',
    name: 'Sales tools',
} as McpServer;

describe('McpServerDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockClear();
        hoisted.updateMutate.mockClear();
    });

    it('creates a server that requires authentication by default', async () => {
        const user = userEvent.setup();

        renderDialog();

        expect(getAuthenticationCheckbox()).toBeChecked();
        expect(screen.queryByRole('checkbox', {name: 'Enforce tool authorization'})).not.toBeInTheDocument();

        await user.type(screen.getByPlaceholderText('Enter server name'), 'Sales tools');
        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.createMutate.mock.calls[0][0]).toEqual({
            input: {
                authenticationRequired: true,
                enabled: false,
                environmentId: '2',
                name: 'Sales tools',
                type: PlatformType.Automation,
                workspaceId: '1',
            },
        });
    });

    it('creates a server without authentication when the checkbox is cleared', async () => {
        const user = userEvent.setup();

        renderDialog();

        await user.type(screen.getByPlaceholderText('Enter server name'), 'Open tools');
        await user.click(getAuthenticationCheckbox());
        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.createMutate.mock.calls[0][0].input.authenticationRequired).toBe(false);
    });

    it('resets and disables tool authorization when authentication is turned off', async () => {
        const user = userEvent.setup();

        renderDialog(existingMcpServer);

        expect(getEnforceToolAuthorizationCheckbox()).toBeChecked();
        expect(getEnforceToolAuthorizationCheckbox()).toBeEnabled();

        await user.click(getAuthenticationCheckbox());

        await waitFor(() => expect(getEnforceToolAuthorizationCheckbox()).not.toBeChecked());

        expect(getEnforceToolAuthorizationCheckbox()).toBeDisabled();

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.updateMutate.mock.calls[0][0]).toEqual({
            id: '7',
            input: {
                authenticationRequired: false,
                enabled: true,
                enforceToolAuthorization: false,
                name: 'Sales tools',
            },
        });
    });

    it('keeps tool authorization disabled for a server that does not require authentication', () => {
        renderDialog({...existingMcpServer, authenticationRequired: false, enforceToolAuthorization: false});

        expect(getAuthenticationCheckbox()).not.toBeChecked();
        expect(getEnforceToolAuthorizationCheckbox()).toBeDisabled();
    });
});
