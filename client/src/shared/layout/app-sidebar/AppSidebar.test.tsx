import {SidebarProvider} from '@/components/ui/sidebar';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {FolderIcon, Layers3Icon, LayoutTemplateIcon, MessagesSquareIcon} from 'lucide-react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AppSidebar} from './AppSidebar';

const hoisted = vi.hoisted(() => ({currentEnvironmentId: 0}));

// AppSidebarFooter pulls in stores/queries; stub it so this test stays focused on nav.
vi.mock('./AppSidebarFooter', () => ({
    AppSidebarFooter: () => null,
}));

// The selector's own rendering is covered by EnvironmentSelect.test.tsx; here it only has to prove it is
// mounted in the rail header, in the form that fits the rail's current width.
vi.mock('@/shared/components/EnvironmentSelect', () => ({
    default: ({variant}: {variant?: string}) => <div data-testid="environment-select">{variant}</div>,
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({currentEnvironmentId: hoisted.currentEnvironmentId}),
}));

const navigation = [
    {href: '/automation/ai-hub', icon: MessagesSquareIcon, name: 'AI Hub'},
    {href: '/automation/projects', icon: FolderIcon, name: 'Projects'},
];

const groupedNavigation = [
    {href: '/automation/projects', icon: FolderIcon, name: 'Projects'},
    {group: 'Deployments', href: '/automation/deployments', icon: Layers3Icon, name: 'Project Deployments'},
    {group: 'Deployments', href: '/automation/api-platform', icon: LayoutTemplateIcon, name: 'API Collections'},
    {group: 'Data', href: '/automation/data-tables', icon: FolderIcon, name: 'Data Tables'},
    {group: 'Data', href: '/automation/knowledge-base', icon: FolderIcon, name: 'Knowledge Base'},
    {group: 'Monitor', href: '/automation/executions', icon: Layers3Icon, name: 'Executions'},
    {href: '/automation/connections', icon: FolderIcon, name: 'Connections'},
];

const renderSidebar = (open = true) =>
    render(
        <MemoryRouter initialEntries={['/automation/projects']}>
            <SidebarProvider defaultOpen={open}>
                <AppSidebar navigation={navigation} />
            </SidebarProvider>
        </MemoryRouter>
    );

describe('AppSidebar', () => {
    beforeEach(() => {
        hoisted.currentEnvironmentId = 0;
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

    it('renders consecutive items sharing a group under one labeled section', () => {
        render(
            <MemoryRouter initialEntries={['/automation/projects']}>
                <SidebarProvider defaultOpen>
                    <AppSidebar navigation={groupedNavigation} />
                </SidebarProvider>
            </MemoryRouter>
        );

        expect(screen.getAllByText('Deployments')).toHaveLength(1);
        expect(screen.getByRole('link', {name: 'Project Deployments'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'API Collections'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Connections'})).toBeInTheDocument();
    });

    it('renders ungrouped items without a group label', () => {
        renderSidebar(true);

        expect(screen.queryByText('Deployments')).not.toBeInTheDocument();
    });

    describe('environment', () => {
        it('renders the compact selector beside the wordmark when the rail is expanded', () => {
            renderSidebar(true);

            expect(screen.getByTestId('environment-select')).toHaveTextContent('compact');
        });

        // Hiding it on the collapsed rail is what made it look missing; the icon-only form fits 56px, so the
        // control survives a collapse instead of disappearing with the wordmark.
        it('falls back to the icon-only selector on the collapsed rail rather than hiding it', () => {
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

        // Routes that render no sidebar (sign-in, the public pages) must not inherit the last tint.
        it('clears the tint when the sidebar unmounts', () => {
            const {unmount} = renderSidebar(true);

            unmount();

            expect(document.documentElement).not.toHaveAttribute('data-environment');
        });
    });

    describe('collapsed rail', () => {
        const renderCollapsed = () =>
            render(
                <MemoryRouter initialEntries={['/automation/projects']}>
                    <SidebarProvider defaultOpen={false}>
                        <AppSidebar navigation={groupedNavigation} />
                    </SidebarProvider>
                </MemoryRouter>
            );

        it('folds a group into a single trigger instead of one icon per item', () => {
            renderCollapsed();

            // The group's items live in a hover flyout, so no link is rendered for them up front.
            expect(screen.queryByRole('link', {name: 'Project Deployments'})).not.toBeInTheDocument();
            expect(screen.queryByRole('link', {name: 'API Collections'})).not.toBeInTheDocument();

            expect(screen.getByRole('button', {name: 'Deployments'})).toBeInTheDocument();
        });

        it('keeps ungrouped items as their own rail links', () => {
            renderCollapsed();

            expect(screen.getByRole('link', {name: 'Projects'})).toBeInTheDocument();
            expect(screen.getByRole('link', {name: 'Connections'})).toBeInTheDocument();
        });

        it('renders a single-item group as a direct link rather than a flyout', () => {
            renderCollapsed();

            expect(screen.getByRole('link', {name: 'Executions'})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Monitor'})).not.toBeInTheDocument();
        });

        // Each flyout closes on a delay so the pointer can wander on its way to the menu. Left uncontrolled,
        // that delay let the outgoing group stay on screen alongside the incoming one when the pointer swept
        // down the rail.
        it('shows only one flyout when the pointer moves between groups', async () => {
            const user = userEvent.setup();

            renderCollapsed();

            await user.hover(screen.getByRole('button', {name: 'Deployments'}));

            expect(await screen.findByRole('link', {name: 'Project Deployments'})).toBeInTheDocument();

            await user.hover(screen.getByRole('button', {name: 'Data'}));

            expect(await screen.findByRole('link', {name: 'Data Tables'})).toBeInTheDocument();
            expect(screen.queryByRole('link', {name: 'Project Deployments'})).not.toBeInTheDocument();
        });

        // Collapsing pulls each group label 32px up and fades it out, leaving an invisible band over the
        // bottom of the icon row above it. Without pointer-events-none it eats that icon's hover, so the
        // flyout only opens from the icon's top edge. jsdom does no layout, so the overlap itself cannot be
        // asserted here — guard the class that makes the hidden label inert.
        it('keeps the hidden group label from swallowing pointer events', () => {
            const {container} = renderCollapsed();

            const label = container.querySelector('[data-sidebar="group-label"]');

            expect(label).toHaveClass('group-data-[collapsible=icon]:pointer-events-none');
        });
    });
});
