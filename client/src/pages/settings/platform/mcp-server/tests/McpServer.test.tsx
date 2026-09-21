import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpServer from '../McpServer';

const hoisted = vi.hoisted(() => ({
    authenticationRequiredData: undefined as undefined | {managementMcpServerAuthenticationRequired: boolean},
    invalidateQueries: vi.fn(),
    updateAuthenticationRequiredMutate: vi.fn(),
    updateAuthenticationRequiredOnSuccess: undefined as undefined | (() => void),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useManagementMcpServerAuthenticationRequiredQuery: () => ({data: hoisted.authenticationRequiredData}),
    useManagementMcpServerUrlQuery: () => ({data: {managementMcpServerUrl: 'https://example.com/mcp/abc'}}),
    useUpdateManagementMcpServerAuthenticationRequiredMutation: ({onSuccess}: {onSuccess: () => void}) => {
        hoisted.updateAuthenticationRequiredOnSuccess = onSuccess;

        return {mutate: hoisted.updateAuthenticationRequiredMutate};
    },
    useUpdateManagementMcpServerUrlMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/layout/Header', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/mcp-server/McpServerConfiguration', () => ({
    default: ({authenticationRequired, mcpServerUrl}: {authenticationRequired: boolean; mcpServerUrl: string}) => (
        <div
            data-authentication-required={String(authenticationRequired)}
            data-mcp-server-url={mcpServerUrl}
            data-testid="mcp-server-configuration"
        />
    ),
}));

describe('McpServer', () => {
    beforeEach(() => {
        hoisted.authenticationRequiredData = undefined;
        hoisted.invalidateQueries.mockClear();
        hoisted.updateAuthenticationRequiredMutate.mockClear();
    });

    it('hides the toggle until the setting loads and defaults the snippet to authenticated', () => {
        render(<McpServer />);

        expect(screen.queryByRole('switch')).not.toBeInTheDocument();
        expect(screen.getByTestId('mcp-server-configuration')).toHaveAttribute('data-authentication-required', 'true');
    });

    it('reflects the stored setting in the toggle and the connection snippet', () => {
        hoisted.authenticationRequiredData = {managementMcpServerAuthenticationRequired: false};

        render(<McpServer />);

        expect(screen.getByRole('switch')).not.toBeChecked();
        expect(screen.getByTestId('mcp-server-configuration')).toHaveAttribute('data-authentication-required', 'false');
    });

    it('saves the toggled value and refreshes the setting afterwards', () => {
        hoisted.authenticationRequiredData = {managementMcpServerAuthenticationRequired: true};

        render(<McpServer />);

        expect(screen.getByRole('switch')).toBeChecked();

        fireEvent.click(screen.getByRole('switch'));

        expect(hoisted.updateAuthenticationRequiredMutate).toHaveBeenCalledWith({authenticationRequired: false});

        hoisted.updateAuthenticationRequiredOnSuccess?.();

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
            queryKey: ['managementMcpServerAuthenticationRequired'],
        });
    });
});
