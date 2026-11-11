import ConnectedUserSheetEmptyState from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetEmptyState';
import useConnectedUserSheetStore from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const renderEmptyState = (nothingToProvision: boolean, onProvision = vi.fn()) =>
    render(
        <MemoryRouter initialEntries={['/embedded/connected-users']}>
            <Routes>
                <Route
                    element={
                        <ConnectedUserSheetEmptyState
                            description="Provision one for this user."
                            icon={<svg />}
                            nothingToProvision={nothingToProvision}
                            nothingToProvisionDescription="Nothing can be provisioned yet."
                            onProvision={onProvision}
                            pageLink={{label: 'Go to MCP Servers', to: '/embedded/mcp-servers'}}
                            provisionLabel="Provision MCP Server"
                            title="No MCP servers yet"
                        />
                    }
                    path="/embedded/connected-users"
                />

                <Route element={<div>mcp servers page</div>} path="/embedded/mcp-servers" />
            </Routes>
        </MemoryRouter>
    );

describe('ConnectedUserSheetEmptyState', () => {
    beforeEach(() => {
        useConnectedUserSheetStore.setState({connectedUserSheetOpen: true});
    });

    it('offers the provisioning button when something can be provisioned', async () => {
        const onProvision = vi.fn();

        renderEmptyState(false, onProvision);

        expect(screen.getByText('Provision one for this user.')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Go to MCP Servers'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Provision MCP Server'}));

        expect(onProvision).toHaveBeenCalledTimes(1);
    });

    it('explains why and opens the page, closing the sheet, when nothing can be provisioned', async () => {
        renderEmptyState(true);

        expect(screen.getByText('Nothing can be provisioned yet.')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Provision MCP Server'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Go to MCP Servers'}));

        expect(screen.getByText('mcp servers page')).toBeInTheDocument();
        expect(useConnectedUserSheetStore.getState().connectedUserSheetOpen).toBe(false);
    });
});
