import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpConnectPanel from '../McpConnectPanel';

vi.mock('@/shared/components/mcp-server/McpServerConfigurationCode', () => ({
    default: ({codeSnippet}: {codeSnippet: string}) => <pre data-testid="code-snippet">{codeSnippet}</pre>,
}));

const MCP_SERVER_URL = 'https://example.com/api/automation/secret/mcp';

const setAuthorizationServerEnabled = (enabled: boolean) =>
    applicationInfoStore.setState({oauth2: {authorizationServer: {enabled}}});

describe('McpConnectPanel', () => {
    beforeEach(() => {
        setAuthorizationServerEnabled(false);
    });

    it('selects Claude by default and switches to Cursor on click', async () => {
        const user = userEvent.setup();

        render(<McpConnectPanel mcpServerUrl={MCP_SERVER_URL} />);

        expect(screen.getByRole('heading', {name: 'Connect to Claude'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /^Claude$/})).toHaveAttribute('aria-pressed', 'true');

        await user.click(screen.getByRole('button', {name: /^Cursor$/}));

        expect(screen.getByRole('heading', {name: 'Connect to Cursor'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /^Cursor$/})).toHaveAttribute('aria-pressed', 'true');
    });

    it('renders a client select on small screens', () => {
        render(<McpConnectPanel mcpServerUrl={MCP_SERVER_URL} />);

        const clientList = screen.getByRole('group', {name: 'Pick your client'});

        expect(clientList).toHaveClass('hidden', 'md:flex');
        expect(screen.getByRole('combobox', {name: 'Client'}).closest('.md\\:hidden')).not.toBeNull();
    });

    it('uses OAuth steps only when the authorization server is enabled', () => {
        const {unmount} = render(<McpConnectPanel authenticationRequired mcpServerUrl={MCP_SERVER_URL} />);

        expect(screen.queryByRole('tab', {name: 'One click'})).not.toBeInTheDocument();
        expect(screen.queryByText('Sign in')).not.toBeInTheDocument();

        unmount();

        setAuthorizationServerEnabled(true);

        render(<McpConnectPanel authenticationRequired mcpServerUrl={MCP_SERVER_URL} />);

        expect(screen.getByRole('tab', {name: 'One click'})).toBeInTheDocument();
        expect(screen.getByText('Sign in')).toBeInTheDocument();
    });

    it('never shows an API key when authentication is not required', () => {
        setAuthorizationServerEnabled(true);

        const {container} = render(<McpConnectPanel authenticationRequired={false} mcpServerUrl={MCP_SERVER_URL} />);

        expect(container.textContent).not.toContain('YOUR_API_KEY');
    });

    it('uses the given API key source', () => {
        render(
            <McpConnectPanel
                apiKeySource="an admin API key created under Settings → Admin API Keys"
                authenticationRequired
                mcpServerUrl={MCP_SERVER_URL}
            />
        );

        expect(screen.getAllByText(/an admin API key created under Settings → Admin API Keys/).length).toBeGreaterThan(
            0
        );
    });
});
