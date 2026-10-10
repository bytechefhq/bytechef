import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpServer from '../McpServer';

const hoisted = vi.hoisted(() => ({
    updateMcpServerUrlMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useManagementMcpServerUrlQuery: () => ({data: {managementMcpServerUrl: 'https://example.com/mcp/abc'}}),
    useUpdateManagementMcpServerUrlMutation: () => ({mutate: hoisted.updateMcpServerUrlMutate}),
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/layout/Header', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/mcp-server/McpServerConfiguration', () => ({
    default: ({
        authenticationRequired,
        mcpServerUrl,
        onRefresh,
    }: {
        authenticationRequired?: boolean;
        mcpServerUrl: string;
        onRefresh: () => void;
    }) => (
        <button
            data-authentication-required={String(authenticationRequired)}
            data-mcp-server-url={mcpServerUrl}
            data-testid="mcp-server-configuration"
            onClick={onRefresh}
        />
    ),
}));

describe('McpServer', () => {
    beforeEach(() => {
        hoisted.updateMcpServerUrlMutate.mockClear();
    });

    it('offers no switch to turn authentication off', () => {
        render(<McpServer />);

        expect(screen.queryByRole('switch')).not.toBeInTheDocument();
        expect(screen.getByTestId('mcp-server-configuration')).toHaveAttribute(
            'data-authentication-required',
            'undefined'
        );
    });

    it('shows the stored server url', () => {
        render(<McpServer />);

        expect(screen.getByTestId('mcp-server-configuration')).toHaveAttribute(
            'data-mcp-server-url',
            'https://example.com/mcp/abc'
        );
    });

    it('rotates the server url on refresh', () => {
        render(<McpServer />);

        fireEvent.click(screen.getByTestId('mcp-server-configuration'));

        expect(hoisted.updateMcpServerUrlMutate).toHaveBeenCalledWith({});
    });
});
