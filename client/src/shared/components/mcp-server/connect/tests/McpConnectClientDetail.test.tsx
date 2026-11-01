import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import McpConnectClientDetail from '../McpConnectClientDetail';
import {MCP_CONNECT_CLIENTS, McpConnectClientI} from '../mcpConnectClients';

vi.mock('@/shared/components/mcp-server/McpServerConfigurationCode', () => ({
    default: ({codeSnippet, onRefresh}: {codeSnippet: string; onRefresh?: () => void}) => (
        <div>
            <pre data-testid="code-snippet">{codeSnippet}</pre>

            {onRefresh && <button onClick={onRefresh}>Regenerate</button>}
        </div>
    ),
}));

const MCP_SERVER_URL = 'https://example.com/api/automation/secret/mcp';
const API_KEY_SOURCE = 'an API key created under Settings';
const THEN_TRY_PROMPT = 'What ByteChef tools do you have? List a few things you could run for me.';

const getClient = (id: string): McpConnectClientI => MCP_CONNECT_CLIENTS.find((client) => client.id === id)!;

const getCodeSnippets = () => screen.queryAllByTestId('code-snippet').map((element) => element.textContent ?? '');

describe('McpConnectClientDetail', () => {
    it('shows the docs link and both tabs for Cursor in API_KEY mode', () => {
        render(
            <McpConnectClientDetail
                apiKeySource={API_KEY_SOURCE}
                client={getClient('cursor')}
                mcpServerUrl={MCP_SERVER_URL}
                mode="API_KEY"
            />
        );

        expect(screen.getByRole('heading', {name: 'Connect to Cursor'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: /Cursor docs/})).toHaveAttribute(
            'href',
            'https://cursor.com/docs/context/mcp'
        );
        expect(screen.getByRole('tab', {name: 'One click'})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Manual'})).toBeInTheDocument();
    });

    it('hides the tab list when a client has one tab', () => {
        render(
            <McpConnectClientDetail
                apiKeySource={API_KEY_SOURCE}
                client={getClient('devin-desktop')}
                mcpServerUrl={MCP_SERVER_URL}
                mode="API_KEY"
            />
        );

        expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
        expect(screen.getByText('Formerly Windsurf')).toBeInTheDocument();
        expect(getCodeSnippets()[0]).toContain(MCP_SERVER_URL);
    });

    it('renders the Add to Cursor action with the install href', () => {
        render(
            <McpConnectClientDetail
                apiKeySource={API_KEY_SOURCE}
                client={getClient('cursor')}
                mcpServerUrl={MCP_SERVER_URL}
                mode="URL_ONLY"
            />
        );

        expect(screen.getByRole('link', {name: /Add to Cursor/})).toHaveAttribute(
            'href',
            expect.stringMatching(/^cursor:\/\/anysphere\.cursor-deeplink\/mcp\/install\?name=ByteChef&config=/)
        );
    });

    it('shows the ChatGPT notice instead of steps in API_KEY mode', () => {
        render(
            <McpConnectClientDetail
                apiKeySource={API_KEY_SOURCE}
                client={getClient('chatgpt')}
                mcpServerUrl={MCP_SERVER_URL}
                mode="API_KEY"
            />
        );

        expect(screen.getByText(/ChatGPT can't send API keys/)).toBeInTheDocument();
        expect(getCodeSnippets()).toHaveLength(0);
    });

    it('copies the Then try prompt', async () => {
        const user = userEvent.setup();

        const writeTextSpy = vi.spyOn(navigator.clipboard, 'writeText');

        render(
            <McpConnectClientDetail
                apiKeySource={API_KEY_SOURCE}
                client={getClient('claude')}
                mcpServerUrl={MCP_SERVER_URL}
                mode="URL_ONLY"
            />
        );

        expect(screen.getByText(THEN_TRY_PROMPT)).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Copy prompt'}));

        expect(writeTextSpy).toHaveBeenCalledWith(THEN_TRY_PROMPT);
    });

    it('rebuilds snippets when the URL changes', () => {
        const newUrl = 'https://example.com/api/automation/regenerated/mcp';

        const {rerender} = render(
            <McpConnectClientDetail
                apiKeySource={API_KEY_SOURCE}
                client={getClient('claude-code')}
                mcpServerUrl={MCP_SERVER_URL}
                mode="URL_ONLY"
            />
        );

        rerender(
            <McpConnectClientDetail
                apiKeySource={API_KEY_SOURCE}
                client={getClient('claude-code')}
                mcpServerUrl={newUrl}
                mode="URL_ONLY"
            />
        );

        expect(getCodeSnippets().some((snippet) => snippet.includes(newUrl))).toBe(true);
        expect(getCodeSnippets().some((snippet) => snippet.includes(MCP_SERVER_URL))).toBe(false);
    });
});
