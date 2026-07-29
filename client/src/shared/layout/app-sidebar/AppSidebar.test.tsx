import {SidebarProvider} from '@/components/ui/sidebar';
import {render, screen} from '@/shared/util/test-utils';
import {FolderIcon, Layers3Icon, LayoutTemplateIcon, MessagesSquareIcon} from 'lucide-react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import {AppSidebar} from './AppSidebar';

// AppSidebarFooter pulls in stores/queries; stub it so this test stays focused on nav.
vi.mock('./AppSidebarFooter', () => ({
    AppSidebarFooter: () => null,
}));

const navigation = [
    {href: '/automation/ai-hub', icon: MessagesSquareIcon, name: 'AI Hub'},
    {href: '/automation/projects', icon: FolderIcon, name: 'Projects'},
];

const groupedNavigation = [
    {href: '/automation/projects', icon: FolderIcon, name: 'Projects'},
    {group: 'Deployments', href: '/automation/deployments', icon: Layers3Icon, name: 'Project Deployments'},
    {group: 'Deployments', href: '/automation/api-platform', icon: LayoutTemplateIcon, name: 'API Collections'},
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
});
