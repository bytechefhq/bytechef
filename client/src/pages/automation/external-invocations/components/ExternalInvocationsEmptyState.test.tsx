import ExternalInvocationsEmptyState from '@/pages/automation/external-invocations/components/ExternalInvocationsEmptyState';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/hooks/useEnvironmentScopeNote', () => ({
    useEnvironmentScopeNote: () => undefined,
}));

const API_PATH = {
    actionLabel: 'Open Getting Started',
    description: 'No setup needed.',
    documentationLabel: 'Component Kit',
    documentationUrl: 'https://docs.example.com/component-kit',
    path: '/embedded/getting-started',
    title: 'Through the Embedded API',
};

const renderEmptyState = (withApiPath: boolean) =>
    render(
        <MemoryRouter initialEntries={['/invocations']}>
            <Routes>
                <Route
                    element={
                        <ExternalInvocationsEmptyState
                            apiPath={withApiPath ? API_PATH : undefined}
                            connectStep={{description: 'Add the URL.', id: 'connect', label: 'Connect an AI agent'}}
                            createMcpServerDescription="Pick the tools."
                            description="Calls are recorded here."
                            documentationUrl="https://docs.example.com"
                            hasMcpServer={false}
                            mcpServersPath="/mcp-servers"
                        />
                    }
                    path="/invocations"
                />

                <Route element={<div>getting started page</div>} path="/embedded/getting-started" />
            </Routes>
        </MemoryRouter>
    );

describe('ExternalInvocationsEmptyState', () => {
    it('shows only the MCP steps when there is no API path', () => {
        renderEmptyState(false);

        expect(screen.getByRole('list', {name: 'Getting started'})).toBeInTheDocument();
        expect(screen.queryByRole('region', {name: 'Through an MCP server'})).not.toBeInTheDocument();
        expect(screen.queryByText('Through the Embedded API')).not.toBeInTheDocument();
    });

    it('splits the MCP and API paths into separate sections', async () => {
        renderEmptyState(true);

        const mcpSection = screen.getByRole('region', {name: 'Through an MCP server'});

        expect(within(mcpSection).getByText('Create an MCP server')).toBeInTheDocument();

        const apiSection = screen.getByRole('region', {name: 'Through the Embedded API'});

        expect(within(apiSection).getByText('No setup needed.')).toBeInTheDocument();
        expect(within(apiSection).getByRole('link', {name: 'Component Kit'})).toHaveAttribute(
            'href',
            'https://docs.example.com/component-kit'
        );

        await userEvent.click(within(apiSection).getByRole('button', {name: 'Open Getting Started'}));

        expect(screen.getByText('getting started page')).toBeInTheDocument();
    });
});
