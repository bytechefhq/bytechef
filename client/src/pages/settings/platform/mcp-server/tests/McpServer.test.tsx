import {fireEvent, render, screen, within} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpServer from '../McpServer';

const hoisted = vi.hoisted(() => ({
    updateMcpServerUrlMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useManagementMcpServerCanonicalUrlQuery: () => ({
        data: {managementMcpServerCanonicalUrl: 'https://app.example.com/api/management/mcp'},
    }),
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
        signInWithByteChefAvailable,
    }: {
        authenticationRequired?: boolean;
        mcpServerUrl: string;
        onRefresh?: () => void;
        signInWithByteChefAvailable?: boolean;
    }) => (
        <div
            data-authentication-required={String(authenticationRequired)}
            data-mcp-server-url={mcpServerUrl}
            data-sign-in-with-bytechef-available={String(signInWithByteChefAvailable)}
            data-testid="mcp-server-configuration"
        >
            {onRefresh && <button onClick={onRefresh}>Regenerate canonical</button>}
        </div>
    ),
}));

vi.mock('@/shared/components/mcp-server/McpServerConfigurationCode', () => ({
    default: ({codeSnippet, onRefresh}: {codeSnippet: string; onRefresh?: () => void}) => (
        <div data-testid="mcp-server-configuration-code">
            <span>{codeSnippet}</span>

            {onRefresh && <button onClick={onRefresh}>Regenerate tenant</button>}
        </div>
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

    it('shows the canonical url under the MCP server URL heading', () => {
        render(<McpServer />);

        expect(screen.getByRole('heading', {name: 'MCP server URL'})).toBeInTheDocument();
        expect(screen.getByTestId('mcp-server-configuration')).toHaveAttribute(
            'data-mcp-server-url',
            'https://app.example.com/api/management/mcp'
        );
    });

    it('tells the canonical url block that signing in with ByteChef is an alternative to the API key', () => {
        render(<McpServer />);

        expect(screen.getByTestId('mcp-server-configuration')).toHaveAttribute(
            'data-sign-in-with-bytechef-available',
            'true'
        );
    });

    it('shows the tenant url under the external identity providers section', () => {
        render(<McpServer />);

        const section = screen.getByRole('group', {name: 'URL for external identity providers'});

        expect(within(section).getByText('https://example.com/mcp/abc')).toBeInTheDocument();
        expect(
            within(section).getByText(/Use this URL when your MCP client signs in through your own identity provider/)
        ).toBeInTheDocument();
    });

    it('offers regeneration only for the tenant url', () => {
        render(<McpServer />);

        expect(screen.queryByRole('button', {name: 'Regenerate canonical'})).not.toBeInTheDocument();
        expect(screen.getAllByRole('button')).toHaveLength(1);
    });

    it('rotates the tenant url on refresh', () => {
        render(<McpServer />);

        fireEvent.click(screen.getByRole('button', {name: 'Regenerate tenant'}));

        expect(hoisted.updateMcpServerUrlMutate).toHaveBeenCalledWith({});
    });
});
