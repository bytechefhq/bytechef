import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import McpServer from '../McpServer';

vi.mock('@/shared/middleware/graphql', () => ({
    useManagementMcpServerCanonicalUrlQuery: () => ({
        data: {managementMcpServerCanonicalUrl: 'https://app.example.com/api/management/mcp'},
    }),
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

describe('McpServer', () => {
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

    it('shows no url for external identity providers', () => {
        render(<McpServer />);

        expect(screen.queryByRole('group', {name: 'URL for external identity providers'})).not.toBeInTheDocument();
        expect(screen.queryAllByRole('button')).toHaveLength(0);
    });
});
