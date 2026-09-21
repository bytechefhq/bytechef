import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import McpServerConfiguration from '../McpServerConfiguration';

vi.mock('@/shared/components/mcp-server/McpServerConfigurationCode', () => ({
    default: ({codeSnippet}: {codeSnippet: string}) => <pre data-testid="code-snippet">{codeSnippet}</pre>,
}));

const MCP_SERVER_URL = 'https://example.com/mcp/abc';

const getCodeSnippets = () => screen.getAllByTestId('code-snippet').map((element) => element.textContent ?? '');

describe('McpServerConfiguration', () => {
    it('adds the Authorization header with the API key placeholder when authentication is required', () => {
        render(<McpServerConfiguration authenticationRequired mcpServerUrl={MCP_SERVER_URL} onRefresh={vi.fn()} />);

        const desktopSnippet = getCodeSnippets().find((snippet) => snippet.includes('mcp-remote'));

        expect(desktopSnippet).toContain('"--header"');
        expect(desktopSnippet).toContain('"Authorization: Bearer YOUR_API_KEY"');
        expect(desktopSnippet).toContain(`"${MCP_SERVER_URL}"`);
        expect(screen.getByText(/Replace YOUR_API_KEY with an API key/)).toBeInTheDocument();
    });

    it('drops the header and the API key instruction when authentication is not required', () => {
        const {container} = render(
            <McpServerConfiguration authenticationRequired={false} mcpServerUrl={MCP_SERVER_URL} onRefresh={vi.fn()} />
        );

        const desktopSnippet = getCodeSnippets().find((snippet) => snippet.includes('mcp-remote'));

        expect(desktopSnippet).toContain(`"${MCP_SERVER_URL}"`);
        expect(desktopSnippet).not.toContain('--header');
        expect(container.textContent).not.toContain('Authorization');
        expect(container.textContent).not.toContain('YOUR_API_KEY');
    });

    it('requires authentication when the prop is omitted', () => {
        render(<McpServerConfiguration mcpServerUrl={MCP_SERVER_URL} onRefresh={vi.fn()} />);

        expect(getCodeSnippets().some((snippet) => snippet.includes('Bearer YOUR_API_KEY'))).toBe(true);
    });

    it('switches the headers block of the URL-based snippet on authenticationRequired', async () => {
        const user = userEvent.setup();

        const {rerender} = render(
            <McpServerConfiguration authenticationRequired mcpServerUrl={MCP_SERVER_URL} onRefresh={vi.fn()} />
        );

        await user.click(screen.getByRole('tab', {name: 'Cursor'}));

        expect(getCodeSnippets()).toHaveLength(1);
        expect(getCodeSnippets()[0]).toContain('"Authorization": "Bearer YOUR_API_KEY"');
        expect(getCodeSnippets()[0]).toContain(`"url": "${MCP_SERVER_URL}"`);

        rerender(
            <McpServerConfiguration authenticationRequired={false} mcpServerUrl={MCP_SERVER_URL} onRefresh={vi.fn()} />
        );

        expect(getCodeSnippets()[0]).not.toContain('"headers"');
        expect(getCodeSnippets()[0]).not.toContain('YOUR_API_KEY');
        expect(getCodeSnippets()[0]).toContain(`"url": "${MCP_SERVER_URL}"`);
    });

    it('explains on the Other tab whether the URL needs an API key', async () => {
        const user = userEvent.setup();

        const {rerender} = render(
            <McpServerConfiguration authenticationRequired mcpServerUrl={MCP_SERVER_URL} onRefresh={vi.fn()} />
        );

        await user.click(screen.getByRole('tab', {name: 'Other'}));

        expect(screen.getByText(/This URL requires an API key/)).toBeInTheDocument();
        expect(getCodeSnippets()).toEqual([MCP_SERVER_URL]);

        rerender(
            <McpServerConfiguration authenticationRequired={false} mcpServerUrl={MCP_SERVER_URL} onRefresh={vi.fn()} />
        );

        expect(screen.getByText(/This server does not require authentication/)).toBeInTheDocument();
        expect(screen.queryByText(/This URL requires an API key/)).not.toBeInTheDocument();
    });
});
