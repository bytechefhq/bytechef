import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AppSidebarWorkspaceSelect} from './AppSidebarWorkspaceSelect';

const hoisted = vi.hoisted(() => ({
    currentWorkspaceId: 1 as number | undefined,
    edition: 'EE',
    setCurrentWorkspaceIdMock: vi.fn(),
    workspaces: [] as {id: number; name: string}[],
}));

vi.mock('@/shared/queries/automation/workspaces.queries', () => ({
    useGetUserWorkspacesQuery: () => ({data: hoisted.workspaces}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: vi.fn((selector: (state: {application: {edition: string} | null}) => unknown) =>
        selector({application: {edition: hoisted.edition}})
    ),
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn((selector: (state: {account: {email: string; id: number}}) => unknown) =>
        selector({account: {email: 'user@localhost.com', id: 1}})
    ),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(
        (selector: (state: {currentWorkspaceId: number | undefined; setCurrentWorkspaceId: () => void}) => unknown) =>
            selector({
                currentWorkspaceId: hoisted.currentWorkspaceId,
                setCurrentWorkspaceId: hoisted.setCurrentWorkspaceIdMock,
            })
    ),
}));

vi.mock('@/pages/home/stores/usePlatformTypeStore', () => ({
    PlatformType: {AUTOMATION: 0, EMBEDDED: 1},
    usePlatformTypeStore: vi.fn((selector: (state: {currentType: number}) => unknown) => selector({currentType: 0})),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 0})
    ),
}));

const renderWorkspaceSelect = (pathname = '/automation/projects') =>
    render(
        <MemoryRouter initialEntries={[pathname]}>
            <Routes>
                <Route element={<AppSidebarWorkspaceSelect />} path={pathname} />

                <Route element={<div>Workspaces settings page</div>} path="/automation/settings/workspaces" />
            </Routes>
        </MemoryRouter>
    );

describe('AppSidebarWorkspaceSelect', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.currentWorkspaceId = 1;
        hoisted.edition = 'EE';
        hoisted.workspaces = [
            {id: 1, name: 'Default'},
            {id: 2, name: 'Marketing'},
        ];
    });

    it('shows the current workspace name in place of the wordmark', () => {
        renderWorkspaceSelect();

        expect(screen.getByRole('button', {name: 'Workspace menu'})).toHaveTextContent('Default');
        expect(screen.queryByText('ByteChef')).not.toBeInTheDocument();
    });

    it('falls back to the wordmark in the community edition', () => {
        hoisted.edition = 'CE';

        renderWorkspaceSelect();

        expect(screen.getByText('ByteChef')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Workspace menu'})).not.toBeInTheDocument();
    });

    it('falls back to the wordmark outside automation', () => {
        renderWorkspaceSelect('/embedded/integrations');

        expect(screen.getByText('ByteChef')).toBeInTheDocument();
    });

    it('switches the current workspace', async () => {
        const user = userEvent.setup();

        renderWorkspaceSelect();

        await user.click(screen.getByRole('button', {name: 'Workspace menu'}));
        await user.click(await screen.findByRole('menuitemradio', {name: 'Marketing'}));

        expect(hoisted.setCurrentWorkspaceIdMock).toHaveBeenCalledWith(2);
    });

    it('links Manage Workspaces to the workspaces settings page', async () => {
        const user = userEvent.setup();

        renderWorkspaceSelect();

        await user.click(screen.getByRole('button', {name: 'Workspace menu'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Manage Workspaces'}));

        expect(await screen.findByText('Workspaces settings page')).toBeInTheDocument();
    });

    it('keeps the current workspace when it is still available', () => {
        renderWorkspaceSelect();

        expect(hoisted.setCurrentWorkspaceIdMock).not.toHaveBeenCalled();
    });

    it('selects the first workspace when none is selected', () => {
        hoisted.currentWorkspaceId = undefined;

        renderWorkspaceSelect();

        expect(hoisted.setCurrentWorkspaceIdMock).toHaveBeenCalledWith(1);
    });

    it('selects the first workspace when the current one is no longer available', () => {
        hoisted.currentWorkspaceId = 99;

        renderWorkspaceSelect();

        expect(hoisted.setCurrentWorkspaceIdMock).toHaveBeenCalledWith(1);
    });

    it('leaves the selection alone while there are no workspaces', () => {
        hoisted.currentWorkspaceId = undefined;
        hoisted.workspaces = [];

        renderWorkspaceSelect();

        expect(hoisted.setCurrentWorkspaceIdMock).not.toHaveBeenCalled();
    });
});
