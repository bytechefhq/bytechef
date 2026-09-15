import {SidebarProvider} from '@/components/ui/sidebar';
import {render, screen} from '@/shared/util/test-utils';
import {fireEvent} from '@testing-library/react';
import {FolderIcon, MessagesSquareIcon, NetworkIcon, ServerCogIcon, ServerIcon} from 'lucide-react';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AppSidebar} from './AppSidebar';

const hoisted = vi.hoisted(() => ({
    accessibleEnvironmentIds: undefined as number[] | undefined,
    currentEnvironmentId: 0,
    setCurrentEnvironmentId: vi.fn(),
}));

vi.mock('@/shared/hooks/useAccessibleEnvironmentIds', () => ({
    useAccessibleEnvironmentIds: () => hoisted.accessibleEnvironmentIds,
}));

// AppSidebarFooter pulls in stores/queries; stub it so this test stays focused on nav.
vi.mock('./AppSidebarFooter', () => ({
    AppSidebarFooter: () => null,
}));

vi.mock('./AppSidebarWorkspaceSelect', () => ({
    AppSidebarWorkspaceSelect: () => <div data-testid="workspace-select" />,
}));

vi.mock('@/shared/components/EnvironmentSelect', () => ({
    default: ({
        onChange,
        variant,
        visibleEnvironmentIds,
    }: {
        onChange?: (environmentId: number) => void;
        variant?: string;
        visibleEnvironmentIds?: number[];
    }) => (
        <div data-testid="environment-select">
            {variant}

            <span data-testid="visible-environments">{visibleEnvironmentIds?.join(',') ?? 'all'}</span>

            <button onClick={() => onChange?.(0)} type="button">
                Switch to development
            </button>

            <button onClick={() => onChange?.(2)} type="button">
                Switch to production
            </button>
        </div>
    ),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            currentEnvironmentId: hoisted.currentEnvironmentId,
            setCurrentEnvironmentId: hoisted.setCurrentEnvironmentId,
        }),
}));

const navigation = [
    {href: '/automation/ai-hub', icon: MessagesSquareIcon, name: 'AI Hub'},
    {href: '/automation/projects', icon: FolderIcon, name: 'Projects'},
];

const serversGroup = {icon: ServerCogIcon, name: 'Servers'};

const groupedNavigation = [
    ...navigation,
    {group: serversGroup, href: '/automation/mcp-servers', icon: ServerIcon, name: 'MCP Servers'},
    {group: serversGroup, href: '/automation/a2a-servers', icon: NetworkIcon, name: 'A2A Servers'},
];

const LocationDisplay = () => {
    const {pathname} = useLocation();

    return <div data-testid="location">{pathname}</div>;
};

const renderSidebar = (open = true, pathname = '/automation/projects', sidebarNavigation = navigation) =>
    render(
        <MemoryRouter initialEntries={[pathname]}>
            <SidebarProvider defaultOpen={open}>
                <AppSidebar navigation={sidebarNavigation} />
            </SidebarProvider>

            <LocationDisplay />
        </MemoryRouter>
    );

describe('AppSidebar', () => {
    beforeEach(() => {
        hoisted.accessibleEnvironmentIds = undefined;
        hoisted.currentEnvironmentId = 0;
        hoisted.setCurrentEnvironmentId.mockClear();
    });

    it('renders a menu item for each navigation entry', () => {
        renderSidebar(true);

        expect(screen.getByRole('link', {name: 'AI Hub'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Projects'})).toBeInTheDocument();
    });

    it('links each item to its href', () => {
        renderSidebar(true);

        expect(screen.getByRole('link', {name: 'AI Hub'})).toHaveAttribute('href', '/automation/ai-hub');
    });

    it('renders the workspace selector in the header', () => {
        renderSidebar(true);

        expect(screen.getByTestId('workspace-select')).toBeInTheDocument();
    });

    it('closes the expanded sidebar from the header button', () => {
        renderSidebar(true);

        fireEvent.click(screen.getByRole('button', {name: 'Close sidebar'}));

        expect(screen.queryByRole('button', {name: 'Close sidebar'})).not.toBeInTheDocument();
        expect(screen.getByTestId('environment-select')).toHaveTextContent('icon');
    });

    describe('environment', () => {
        it('renders the icon-only selector between the workspace selector and the close button', () => {
            renderSidebar(true);

            expect(screen.getByTestId('environment-select')).toHaveTextContent('icon');
        });

        it('falls back to the icon-only selector on the collapsed rail', () => {
            renderSidebar(false);

            expect(screen.getByTestId('environment-select')).toHaveTextContent('icon');
        });

        it('tints the document element with the selected environment', () => {
            renderSidebar(true);

            expect(document.documentElement).toHaveAttribute('data-environment', 'development');
        });

        it('retints when the selected environment changes', () => {
            const {rerender} = renderSidebar(true);

            hoisted.currentEnvironmentId = 2;

            rerender(
                <MemoryRouter initialEntries={['/automation/projects']}>
                    <SidebarProvider defaultOpen>
                        <AppSidebar navigation={navigation} />
                    </SidebarProvider>
                </MemoryRouter>
            );

            expect(document.documentElement).toHaveAttribute('data-environment', 'production');
        });

        it('clears the tint when the sidebar unmounts', () => {
            const {unmount} = renderSidebar(true);

            unmount();

            expect(document.documentElement).not.toHaveAttribute('data-environment');
        });

        it('redirects the workflow editor to deployments when switching away from development', () => {
            renderSidebar(true, '/automation/projects/1/project-workflows/2');

            fireEvent.click(screen.getByRole('button', {name: 'Switch to production'}));

            expect(screen.getByTestId('location')).toHaveTextContent('/automation/deployments');
        });

        it('redirects the integration editor to configurations when switching away from development', () => {
            renderSidebar(true, '/embedded/integrations/1/integration-workflows/2');

            fireEvent.click(screen.getByRole('button', {name: 'Switch to production'}));

            expect(screen.getByTestId('location')).toHaveTextContent('/embedded/configurations');
        });

        it('stays on environment-aware pages when switching environments', () => {
            renderSidebar(true, '/automation/connections');

            fireEvent.click(screen.getByRole('button', {name: 'Switch to production'}));

            expect(screen.getByTestId('location')).toHaveTextContent('/automation/connections');
        });

        it('offers only the environments the member has access to', () => {
            hoisted.accessibleEnvironmentIds = [0, 2];

            renderSidebar(true);

            expect(screen.getByTestId('visible-environments')).toHaveTextContent('0,2');
        });

        it('offers every environment while access is unknown', () => {
            renderSidebar(true);

            expect(screen.getByTestId('visible-environments')).toHaveTextContent('all');
        });

        it('switches to the first accessible environment when the selected one is not accessible', () => {
            hoisted.accessibleEnvironmentIds = [2];

            renderSidebar(true, '/automation/projects');

            expect(hoisted.setCurrentEnvironmentId).toHaveBeenCalledWith(2);
            expect(screen.getByTestId('location')).toHaveTextContent('/automation/deployments');
        });

        it('keeps the selected environment when it is accessible', () => {
            hoisted.accessibleEnvironmentIds = [0, 2];

            renderSidebar(true);

            expect(hoisted.setCurrentEnvironmentId).not.toHaveBeenCalled();
        });

        it('stays in the workflow editor when switching to development', () => {
            renderSidebar(true, '/automation/projects/1/project-workflows/2');

            fireEvent.click(screen.getByRole('button', {name: 'Switch to development'}));

            expect(screen.getByTestId('location')).toHaveTextContent('/automation/projects/1/project-workflows/2');
        });
    });

    describe('groups', () => {
        it('renders a group as a closed parent row when the route is outside it', () => {
            renderSidebar(true, '/automation/projects', groupedNavigation);

            expect(screen.getByRole('button', {name: 'Servers'})).toHaveAttribute('aria-expanded', 'false');
            expect(screen.queryByRole('link', {name: 'MCP Servers'})).not.toBeInTheDocument();
            expect(screen.getByRole('link', {name: 'Projects'})).toBeInTheDocument();
        });

        it('opens a group from its parent row', () => {
            renderSidebar(true, '/automation/projects', groupedNavigation);

            fireEvent.click(screen.getByRole('button', {name: 'Servers'}));

            expect(screen.getByRole('link', {name: 'MCP Servers'})).toHaveAttribute('href', '/automation/mcp-servers');
            expect(screen.getByRole('link', {name: 'A2A Servers'})).toHaveAttribute('href', '/automation/a2a-servers');
        });

        it('opens the group holding the current route', () => {
            renderSidebar(true, '/automation/a2a-servers', groupedNavigation);

            expect(screen.getByRole('button', {name: 'Servers'})).toHaveAttribute('aria-expanded', 'true');
            expect(screen.getByRole('link', {name: 'A2A Servers'})).toHaveAttribute('data-active', 'true');
        });

        it('keeps the group holding the current route closed once the user closes it', () => {
            renderSidebar(true, '/automation/mcp-servers', groupedNavigation);

            fireEvent.click(screen.getByRole('button', {name: 'Servers'}));

            expect(screen.getByRole('button', {name: 'Servers'})).toHaveAttribute('aria-expanded', 'false');
            expect(screen.getByRole('button', {name: 'Servers'})).toHaveAttribute('data-active', 'true');
            expect(screen.queryByRole('link', {name: 'MCP Servers'})).not.toBeInTheDocument();
        });

        it('folds a group into one rail icon on the collapsed sidebar', () => {
            renderSidebar(false, '/automation/projects', groupedNavigation);

            expect(screen.getByRole('button', {name: 'Servers'})).toBeInTheDocument();
            expect(screen.queryByRole('link', {name: 'MCP Servers'})).not.toBeInTheDocument();
            expect(screen.getByRole('link', {name: 'Projects'})).toBeInTheDocument();
        });

        it('renders a one-item group as a plain row on the collapsed sidebar', () => {
            renderSidebar(false, '/automation/projects', [...navigation, groupedNavigation[2]]);

            expect(screen.queryByRole('button', {name: 'Servers'})).not.toBeInTheDocument();
            expect(screen.getByRole('link', {name: 'MCP Servers'})).toBeInTheDocument();
        });
    });
});
