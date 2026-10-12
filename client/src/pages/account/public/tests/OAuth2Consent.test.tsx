import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

import OAuth2Consent from '../OAuth2Consent';

const hoisted = vi.hoisted(() => ({
    consentClientQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useOauth2ConsentClientQuery: hoisted.consentClientQuery,
}));

const renderOAuth2Consent = (search: string) => {
    render(
        <MemoryRouter initialEntries={[`/oauth2/consent${search}`]}>
            <Routes>
                <Route element={<OAuth2Consent />} path="/oauth2/consent" />
            </Routes>
        </MemoryRouter>
    );
};

beforeEach(() => {
    hoisted.consentClientQuery.mockReturnValue({data: undefined});

    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

it('should render the client id and a label for each requested scope', () => {
    renderOAuth2Consent('?client_id=my-client&scope=mcp:automation mcp:management&state=xyz');

    expect(screen.getByText('my-client')).toBeInTheDocument();
    expect(screen.getByText('Access the Automation MCP server')).toBeInTheDocument();
    expect(screen.getByText('Access the Management MCP server')).toBeInTheDocument();
});

it('should render the raw scope string for unknown scopes', () => {
    renderOAuth2Consent('?client_id=my-client&scope=custom:scope&state=xyz');

    expect(screen.getByText('custom:scope')).toBeInTheDocument();
});

it('should render the invalid consent request fallback when client_id is missing', () => {
    renderOAuth2Consent('?scope=mcp:automation&state=xyz');

    expect(screen.getByText('Invalid consent request')).toBeInTheDocument();
});

it('should render the invalid consent request fallback when state is missing', () => {
    renderOAuth2Consent('?client_id=my-client&scope=mcp:automation');

    expect(screen.getByText('Invalid consent request')).toBeInTheDocument();
});

it('should build an Allow form posting to the authorization endpoint with hidden client_id, state and scope inputs', () => {
    renderOAuth2Consent('?client_id=my-client&scope=mcp:automation mcp:management&state=xyz');

    const allowButton = screen.getByRole('button', {name: 'Allow'});
    const allowForm = allowButton.closest('form');

    expect(allowForm).not.toBeNull();
    expect(allowForm).toHaveAttribute('action', '/oauth2/authorize');
    expect(allowForm).toHaveAttribute('method', 'post');

    const clientIdInput = allowForm?.querySelector('input[name="client_id"]');
    const stateInput = allowForm?.querySelector('input[name="state"]');
    const scopeInputs = allowForm?.querySelectorAll('input[name="scope"]');

    expect(clientIdInput).toHaveValue('my-client');
    expect(stateInput).toHaveValue('xyz');
    expect(scopeInputs).toHaveLength(2);
});

it('should build a Deny form posting to the authorization endpoint with no scope inputs', () => {
    renderOAuth2Consent('?client_id=my-client&scope=mcp:automation&state=xyz');

    const denyButton = screen.getByRole('button', {name: 'Deny'});
    const denyForm = denyButton.closest('form');

    expect(denyForm).not.toBeNull();
    expect(denyForm).toHaveAttribute('action', '/oauth2/authorize');
    expect(denyForm).toHaveAttribute('method', 'post');

    const scopeInputs = denyForm?.querySelectorAll('input[name="scope"]');

    expect(scopeInputs).toHaveLength(0);
});

it('should show the registered client name and redirect hosts marked as unverified', () => {
    hoisted.consentClientQuery.mockReturnValue({
        data: {
            oauth2ConsentClient: {
                clientId: 'my-client',
                clientName: 'Claude',
                redirectHosts: ['claude.ai', 'localhost'],
            },
        },
    });

    renderOAuth2Consent('?client_id=my-client&scope=mcp:automation&state=xyz');

    expect(hoisted.consentClientQuery).toHaveBeenCalledWith({clientId: 'my-client'}, expect.anything());
    expect(screen.getByText('Claude')).toBeInTheDocument();
    expect(screen.getByText(/name is self-declared and unverified/)).toBeInTheDocument();
    expect(screen.getByText('claude.ai, localhost')).toBeInTheDocument();
});

it('should remove the hidden input of an unchecked scope and disable Allow once none is left', async () => {
    renderOAuth2Consent('?client_id=my-client&scope=mcp:automation mcp:management&state=xyz');

    const allowButton = screen.getByRole('button', {name: 'Allow'});
    const allowForm = allowButton.closest('form');

    await userEvent.click(screen.getByRole('checkbox', {name: 'Access the Automation MCP server'}));

    const remainingScopeInputs = allowForm?.querySelectorAll('input[name="scope"]');

    expect(remainingScopeInputs).toHaveLength(1);
    expect(remainingScopeInputs?.[0]).toHaveValue('mcp:management');
    expect(allowButton).toBeEnabled();

    await userEvent.click(screen.getByRole('checkbox', {name: 'Access the Management MCP server'}));

    expect(allowForm?.querySelectorAll('input[name="scope"]')).toHaveLength(0);
    expect(allowButton).toBeDisabled();
});

it('should disable both buttons and the scopes after Allow is clicked so the request is sent once', async () => {
    const preventNavigation = (event: Event) => event.preventDefault();

    window.addEventListener('submit', preventNavigation);

    renderOAuth2Consent('?client_id=my-client&scope=mcp:automation&state=xyz');

    await userEvent.click(screen.getByRole('button', {name: 'Allow'}));

    expect(screen.getByRole('button', {name: 'Allow'})).toBeDisabled();
    expect(screen.getByRole('button', {name: 'Deny'})).toBeDisabled();
    expect(screen.getByRole('checkbox', {name: 'Access the Automation MCP server'})).toBeDisabled();

    window.removeEventListener('submit', preventNavigation);
});

it('should disable both buttons after Deny is clicked', async () => {
    const preventNavigation = (event: Event) => event.preventDefault();

    window.addEventListener('submit', preventNavigation);

    renderOAuth2Consent('?client_id=my-client&scope=mcp:automation&state=xyz');

    await userEvent.click(screen.getByRole('button', {name: 'Deny'}));

    expect(screen.getByRole('button', {name: 'Allow'})).toBeDisabled();
    expect(screen.getByRole('button', {name: 'Deny'})).toBeDisabled();

    window.removeEventListener('submit', preventNavigation);
});
