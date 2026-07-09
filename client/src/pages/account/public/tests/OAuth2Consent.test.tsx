import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, expect, it} from 'vitest';

import OAuth2Consent from '../OAuth2Consent';

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
