import CommandBarDialog from '@/components/CommandBar/CommandBarDialog';
import {useCommandBarStore} from '@/shared/command-bar/useCommandBarStore';
import {useCommandRecentsStore} from '@/shared/command-bar/useCommandRecentsStore';
import {registerCommandSource, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {DatabaseIcon} from 'lucide-react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {navigateMock} = vi.hoisted(() => ({navigateMock: vi.fn()}));

vi.mock('react-router-dom', async () => ({
    ...(await vi.importActual<typeof import('react-router-dom')>('react-router-dom')),
    useNavigate: () => navigateMock,
}));

const renderDialog = () => render(<CommandBarDialog />, {wrapper: MemoryRouter});

describe('CommandBarDialog', () => {
    beforeEach(() => {
        navigateMock.mockClear();
        useCommandSourceRegistry.getState().reset();
        useCommandRecentsStore.getState().reset();
        useCommandBarStore.getState().close();
        useCommandBarStore.getState().setOpen(true);
        authenticationStore.setState({account: {id: 1} as never, authenticated: true});
    });

    it('renders registered commands under their group heading', () => {
        registerCommandSource({
            getCommands: () => [
                {
                    actions: [{to: '/automation/projects', type: 'navigate'}],
                    group: 'Navigation',
                    id: 'nav.projects',
                    title: 'Go to Projects',
                },
            ],
            id: 'test',
        });

        renderDialog();

        expect(screen.getByText('Navigation')).toBeInTheDocument();
        expect(screen.getByText('Go to Projects')).toBeInTheDocument();
    });

    it('runs the actions of a selected command', async () => {
        registerCommandSource({
            getCommands: () => [
                {
                    actions: [{to: '/automation/projects', type: 'navigate'}],
                    id: 'nav.projects',
                    title: 'Go to Projects',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Go to Projects'));

        expect(navigateMock).toHaveBeenCalledWith('/automation/projects');
    });

    it('orders groups by GROUP_ORDER rather than alphabetically', () => {
        // 'API Platform' sorts before 'Workflows' alphabetically, but GROUP_ORDER places 'Workflows' first and
        // 'API Platform' sixth, and 'Navigation' last of all. A naive alphabetical sort would render these as
        // API Platform, Navigation, Workflows -- the opposite of what this test asserts -- so this only passes
        // when compareGroups (not localeCompare) actually drives the ordering.
        registerCommandSource({
            getCommands: () => [
                {
                    actions: [{to: '/automation/workflows', type: 'navigate'}],
                    group: 'Workflows',
                    id: 'open.workflow',
                    title: 'Open workflow',
                },
                {
                    actions: [{to: '/automation/api-platform', type: 'navigate'}],
                    group: 'API Platform',
                    id: 'open.api-platform',
                    title: 'Open API platform',
                },
                {
                    actions: [{to: '/automation/projects', type: 'navigate'}],
                    group: 'Navigation',
                    id: 'nav.projects',
                    title: 'Go to Projects',
                },
            ],
            id: 'test',
        });

        renderDialog();

        const workflows = screen.getByText('Workflows');
        const apiPlatform = screen.getByText('API Platform');
        const navigation = screen.getByText('Navigation');

        expect(workflows.compareDocumentPosition(apiPlatform) & 4).toBe(4);
        expect(apiPlatform.compareDocumentPosition(navigation) & 4).toBe(4);
        expect(workflows.compareDocumentPosition(navigation) & 4).toBe(4);
    });

    it('closes when a command is selected', async () => {
        registerCommandSource({
            getCommands: () => [
                {
                    actions: [{to: '/automation/projects', type: 'navigate'}],
                    id: 'nav.projects',
                    title: 'Go to Projects',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Go to Projects'));

        expect(useCommandBarStore.getState().open).toBe(false);
    });

    it('pushes a sub-mode and swaps the placeholder', async () => {
        registerCommandSource({
            getCommands: () => [
                {
                    children: {
                        minQueryLength: 0,
                        placeholder: 'Search by workflow name...',
                        resolve: async () => [
                            {
                                actions: [{to: '/automation/projects/1/project-workflows/2', type: 'navigate'}],
                                id: 'wf.2',
                                title: 'My workflow',
                            },
                        ],
                    },
                    id: 'workflow.open',
                    title: 'Open workflow',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Open workflow'));

        expect(await screen.findByPlaceholderText('Search by workflow name...')).toBeInTheDocument();
        expect(await screen.findByText('My workflow')).toBeInTheDocument();
    });

    it('pops the sub-mode when Backspace is pressed on an empty input', async () => {
        registerCommandSource({
            getCommands: () => [
                {
                    children: {minQueryLength: 0, placeholder: 'Search by workflow name...', resolve: async () => []},
                    id: 'workflow.open',
                    title: 'Open workflow',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Open workflow'));
        await userEvent.type(screen.getByPlaceholderText('Search by workflow name...'), '{backspace}');

        expect(useCommandBarStore.getState().stack).toHaveLength(0);
    });

    it('deletes a character instead of popping when Backspace is pressed on a non-empty query', async () => {
        registerCommandSource({
            getCommands: () => [
                {
                    children: {minQueryLength: 0, placeholder: 'Search by workflow name...', resolve: async () => []},
                    id: 'workflow.open',
                    title: 'Open workflow',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Open workflow'));

        const input = screen.getByPlaceholderText('Search by workflow name...');

        await userEvent.type(input, 'ab{backspace}');

        expect(useCommandBarStore.getState().stack).toHaveLength(1);
        expect(input).toHaveValue('a');
    });

    it('pops the sub-mode when Escape is pressed, leaving the dialog open', async () => {
        registerCommandSource({
            getCommands: () => [
                {
                    children: {minQueryLength: 0, placeholder: 'Search by workflow name...', resolve: async () => []},
                    id: 'workflow.open',
                    title: 'Open workflow',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Open workflow'));
        await userEvent.keyboard('{Escape}');

        expect(useCommandBarStore.getState().stack).toHaveLength(0);
        expect(useCommandBarStore.getState().open).toBe(true);
    });

    it('closes the dialog when Escape is pressed at the root', async () => {
        registerCommandSource({
            getCommands: () => [
                {
                    actions: [{to: '/automation/projects', type: 'navigate'}],
                    id: 'nav.projects',
                    title: 'Go to Projects',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.keyboard('{Escape}');

        expect(useCommandBarStore.getState().open).toBe(false);
    });

    it('closes the whole palette (rather than popping) when clicking outside while nested', async () => {
        // Radix's DialogContent fires onOpenChange(false) for Escape, outside-click, and the close button alike --
        // popping only belongs to Escape. A click outside the palette while a sub-mode is active must close it
        // entirely, not just return to the root command list.
        registerCommandSource({
            getCommands: () => [
                {
                    children: {minQueryLength: 0, placeholder: 'Search by workflow name...', resolve: async () => []},
                    id: 'workflow.open',
                    title: 'Open workflow',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Open workflow'));

        expect(await screen.findByPlaceholderText('Search by workflow name...')).toBeInTheDocument();

        // Radix disables pointer-events on <body> for a modal dialog, so a real outside-click has to land on the
        // overlay -- the one element Radix explicitly keeps clickable as the dialog's own dismiss affordance.
        const overlay = document.querySelector('[data-slot="dialog-overlay"]');

        expect(overlay).not.toBeNull();

        await userEvent.click(overlay!);

        expect(useCommandBarStore.getState().open).toBe(false);
    });

    it('renders recents first, above the registered commands', async () => {
        useCommandRecentsStore.getState().reset();
        useCommandRecentsStore.getState().addRecent('1', {
            actions: [{to: '/automation/projects/1/project-workflows/2', type: 'navigate'}],
            id: 'resource.WORKFLOW.2',
            title: 'My workflow',
        });

        registerCommandSource({
            getCommands: () => [
                {
                    actions: [{to: '/automation/projects', type: 'navigate'}],
                    group: 'Navigation',
                    id: 'nav.projects',
                    title: 'Go to Projects',
                },
            ],
            id: 'test',
        });

        renderDialog();

        const recentHeading = screen.getByText('Recent');
        const navigationHeading = screen.getByText('Navigation');

        // Node.DOCUMENT_POSITION_FOLLOWING === 4: Navigation comes after Recent in document order.
        expect(recentHeading.compareDocumentPosition(navigationHeading) & 4).toBe(4);
    });

    it('replays a recent command', async () => {
        useCommandRecentsStore.getState().reset();
        useCommandRecentsStore.getState().addRecent('1', {
            actions: [{to: '/automation/projects/1/project-workflows/2', type: 'navigate'}],
            id: 'resource.WORKFLOW.2',
            title: 'My workflow',
        });

        renderDialog();

        await userEvent.click(screen.getByText('My workflow'));

        expect(navigateMock).toHaveBeenCalledWith('/automation/projects/1/project-workflows/2');
    });

    it('falls back to the generic arrow icon when the recorded command is no longer registered', async () => {
        // No registerCommandSource call here -- the registry knows nothing about 'resource.WORKFLOW.2', which is the
        // whole point: a naive implementation that always shows the recent's own icon field (never set, since it is
        // not persisted) would render an icon-less item instead of the fallback, or would crash on a missing icon.
        useCommandRecentsStore.getState().addRecent('1', {
            actions: [{to: '/automation/projects/1/project-workflows/2', type: 'navigate'}],
            id: 'resource.WORKFLOW.2',
            title: 'My workflow',
        });

        renderDialog();

        const row = (await screen.findByText('My workflow')).closest('[cmdk-item]');

        expect(row?.querySelector('svg')).toHaveClass('lucide-arrow-right');
    });

    it('resolves the icon from the live registry when the recorded command is still registered', async () => {
        useCommandRecentsStore.getState().addRecent('1', {
            actions: [{to: '/automation/projects/1/project-workflows/2', type: 'navigate'}],
            id: 'resource.WORKFLOW.2',
            title: 'My workflow',
        });

        registerCommandSource({
            getCommands: () => [
                {
                    actions: [{to: '/automation/projects/1/project-workflows/2', type: 'navigate'}],
                    icon: DatabaseIcon,
                    id: 'resource.WORKFLOW.2',
                    title: 'My workflow',
                },
            ],
            id: 'test',
        });

        renderDialog();

        const rows = await screen.findAllByText('My workflow');
        const recentRow = rows[0].closest('[cmdk-item]');

        // The persisted RecentCommandI carries no icon field, so a correct implementation must look the command up
        // by id in the live registry to find this. A wrong implementation that always falls back to the generic
        // arrow would still pass the fallback test above but fail this one.
        expect(recentRow?.querySelector('svg')).toHaveClass('lucide-database');
    });

    it('shows "No results found." instead of a blank group when a sub-mode search returns nothing', async () => {
        registerCommandSource({
            getCommands: () => [
                {
                    children: {
                        minQueryLength: 0,
                        placeholder: 'Search by workflow name...',
                        resolve: async () => [],
                    },
                    id: 'workflow.open',
                    title: 'Open workflow',
                },
            ],
            id: 'test',
        });

        renderDialog();

        await userEvent.click(screen.getByText('Open workflow'));

        expect(await screen.findByPlaceholderText('Search by workflow name...')).toBeInTheDocument();
        expect(await screen.findByText('No results found.')).toBeInTheDocument();
    });

    it('still shows a recent command after typing a query that matches its title', async () => {
        // Recents are given a `recent-${id}` cmdk value to avoid colliding with the live registry entry's own
        // value, but cmdk scores against value + keywords -- without the title in keywords, that synthetic value
        // scores 0 against any typed query and the recent disappears the moment the user types.
        useCommandRecentsStore.getState().reset();
        useCommandRecentsStore.getState().addRecent('1', {
            actions: [{to: '/automation/datatables/9', type: 'navigate'}],
            id: 'resource.DATA_TABLE.9',
            title: 'Customer data table',
        });

        renderDialog();

        expect(screen.getByText('Customer data table')).toBeInTheDocument();

        await userEvent.type(screen.getByPlaceholderText('Type a command or search...'), 'data table');

        expect(screen.getByText('Customer data table')).toBeInTheDocument();
    });

    it('does not render the Recent group inside a sub-mode', async () => {
        useCommandRecentsStore.getState().addRecent('1', {
            actions: [{to: '/automation/projects/1/project-workflows/2', type: 'navigate'}],
            id: 'resource.WORKFLOW.2',
            title: 'My workflow',
        });

        registerCommandSource({
            getCommands: () => [
                {
                    children: {
                        minQueryLength: 0,
                        placeholder: 'Search by workflow name...',
                        resolve: async () => [],
                    },
                    id: 'workflow.open',
                    title: 'Open workflow',
                },
            ],
            id: 'test',
        });

        renderDialog();

        // Sanity check: recents do render at the root, so the group's absence below is caused by entering the
        // sub-mode, not by some other reason the group never renders at all.
        expect(screen.getByText('Recent')).toBeInTheDocument();

        await userEvent.click(screen.getByText('Open workflow'));

        expect(await screen.findByPlaceholderText('Search by workflow name...')).toBeInTheDocument();
        expect(screen.queryByText('Recent')).not.toBeInTheDocument();
    });
});
