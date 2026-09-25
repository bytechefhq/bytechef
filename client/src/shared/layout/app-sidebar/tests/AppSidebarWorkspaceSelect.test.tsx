import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AppSidebarWorkspaceSelect} from '../AppSidebarWorkspaceSelect';

const hoisted = vi.hoisted(() => ({
    currentType: 0 as number | undefined,
    currentWorkspaceId: 1 as number | undefined,
    edition: 'EE',
    isTenantAdmin: true,
    setCurrentTypeMock: vi.fn(),
    setCurrentWorkspaceIdMock: vi.fn(),
    workspaces: [] as {id: number; name: string}[],
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
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
    usePlatformTypeStore: vi.fn(
        (selector: (state: {currentType: number | undefined; setCurrentType: () => void}) => unknown) =>
            selector({currentType: hoisted.currentType, setCurrentType: hoisted.setCurrentTypeMock})
    ),
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

                <Route element={<div>Approval tasks page</div>} path="/automation/approval-tasks" />

                <Route element={<div>Workspaces settings page</div>} path="/automation/settings/workspaces" />

                <Route element={<div>Embedded integrations page</div>} path="/embedded/integrations" />
            </Routes>
        </MemoryRouter>
    );

describe('AppSidebarWorkspaceSelect', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.currentType = 0;
        hoisted.currentWorkspaceId = 1;
        hoisted.edition = 'EE';
        hoisted.isTenantAdmin = true;
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

    it('shows the Embedded title in embedded mode', () => {
        renderWorkspaceSelect('/embedded/integrations');

        expect(screen.getByText('Embedded')).toBeInTheDocument();
        expect(screen.queryByText('ByteChef')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Workspace menu'})).not.toBeInTheDocument();
    });

    it('shows the Automation title when there are no workspaces', () => {
        hoisted.workspaces = [];

        renderWorkspaceSelect();

        expect(screen.getByText('Automation')).toBeInTheDocument();
    });

    it('does not offer the mode switcher in the community edition', () => {
        hoisted.edition = 'CE';

        renderWorkspaceSelect();

        expect(screen.queryByRole('button', {name: 'Switch mode'})).not.toBeInTheDocument();
    });

    it('switches to embedded mode from the logo menu', async () => {
        const user = userEvent.setup();

        renderWorkspaceSelect();

        await user.click(screen.getByRole('button', {name: 'Switch mode'}));
        await user.click(await screen.findByRole('menuitem', {name: /Embedded/}));

        expect(hoisted.setCurrentTypeMock).toHaveBeenCalledWith(1);
        expect(await screen.findByText('Embedded integrations page')).toBeInTheDocument();
    });

    it('stays put when the current mode is selected again', async () => {
        const user = userEvent.setup();

        renderWorkspaceSelect();

        await user.click(screen.getByRole('button', {name: 'Switch mode'}));
        await user.click(await screen.findByRole('menuitem', {name: /Automation/}));

        expect(hoisted.setCurrentTypeMock).not.toHaveBeenCalled();
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

    it('hides Manage Workspaces from a user who is not a tenant admin', async () => {
        hoisted.isTenantAdmin = false;

        const user = userEvent.setup();

        renderWorkspaceSelect();

        await user.click(screen.getByRole('button', {name: 'Workspace menu'}));

        expect(await screen.findByRole('menuitemradio', {name: 'Marketing'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Manage Workspaces'})).not.toBeInTheDocument();
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

    it('opens Approval Tasks from the mode menu in automation mode', async () => {
        const user = userEvent.setup();

        renderWorkspaceSelect();

        await user.click(screen.getByRole('button', {name: 'Switch mode'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Approval Tasks'}));

        expect(await screen.findByText('Approval tasks page')).toBeInTheDocument();
    });

    it('hides Approval Tasks from the mode menu in embedded mode', async () => {
        const user = userEvent.setup();

        renderWorkspaceSelect('/embedded/integrations');

        await user.click(screen.getByRole('button', {name: 'Switch mode'}));

        expect(await screen.findByRole('menuitem', {name: /Embedded/})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Approval Tasks'})).not.toBeInTheDocument();
    });

    it('offers Approval Tasks on an Organization settings page when no mode is remembered', async () => {
        hoisted.currentType = undefined;

        const user = userEvent.setup();

        renderWorkspaceSelect('/settings/users');

        await user.click(screen.getByRole('button', {name: 'Switch mode'}));

        expect(await screen.findByRole('menuitem', {name: 'Approval Tasks'})).toBeInTheDocument();
    });

    it('offers only Approval Tasks from the logo menu in the community edition', async () => {
        hoisted.edition = 'CE';

        const user = userEvent.setup();

        renderWorkspaceSelect();

        await user.click(screen.getByRole('button', {name: 'ByteChef menu'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Approval Tasks'}));

        expect(screen.queryByText('Mode')).not.toBeInTheDocument();
        expect(await screen.findByText('Approval tasks page')).toBeInTheDocument();
    });
});
