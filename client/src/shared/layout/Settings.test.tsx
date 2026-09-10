import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Settings, {SettingsNavItemI} from './Settings';

const hoisted = vi.hoisted(() => ({
    enabledFeatureFlags: [] as string[],
}));

vi.mock('@/shared/layout/Header', () => ({
    default: () => null,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({leftSidebarBody}: {leftSidebarBody: ReactNode}) => <div>{leftSidebarBody}</div>,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => false,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => (featureFlag: string) => hoisted.enabledFeatureFlags.includes(featureFlag),
}));

const renderSettings = (sidebarNavItems: SettingsNavItemI[], pathname = '/') =>
    render(
        <MemoryRouter initialEntries={[pathname]}>
            <Settings sidebarNavItems={sidebarNavItems} />
        </MemoryRouter>
    );

const aiNavGroup: SettingsNavItemI = {
    items: [
        {href: 'ai-providers', title: 'Providers'},
        {href: 'ai/skills', title: 'Skills'},
    ],
    title: 'AI',
};

describe('Settings', () => {
    beforeEach(() => {
        hoisted.enabledFeatureFlags = [];
    });

    it('hides a section heading when every item below it is hidden by a feature flag', () => {
        renderSettings([
            {href: '/automation/settings/workspaces', title: 'Workspaces'},
            {title: 'Current Workspace'},
            {href: 'git-configuration', title: 'Git Configuration'},
            {href: 'workspace-api-keys', title: 'API Keys'},
            {title: 'Organization'},
            {href: 'users', title: 'Users'},
        ]);

        expect(screen.queryByText('Current Workspace')).not.toBeInTheDocument();
        expect(screen.getByText('Organization')).toBeInTheDocument();
        expect(screen.getByText('Users')).toBeInTheDocument();
    });

    it('keeps a section heading when an item below it is visible', () => {
        hoisted.enabledFeatureFlags = ['ff-1039'];

        renderSettings([
            {title: 'Current Workspace'},
            {href: 'git-configuration', title: 'Git Configuration'},
            {title: 'Organization'},
            {href: 'users', title: 'Users'},
        ]);

        expect(screen.getByText('Current Workspace')).toBeInTheDocument();
        expect(screen.getByText('Git Configuration')).toBeInTheDocument();
    });

    it('hides a trailing section heading that has no items at all', () => {
        renderSettings([{href: 'users', title: 'Users'}, {title: 'Organization'}]);

        expect(screen.queryByText('Organization')).not.toBeInTheDocument();
        expect(screen.getByText('Users')).toBeInTheDocument();
    });

    it('opens the nav group holding the current route', () => {
        renderSettings([{title: 'Organization'}, aiNavGroup], '/automation/settings/ai/skills');

        expect(screen.getByRole('link', {name: 'Providers'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Skills'})).toBeInTheDocument();
    });

    it('keeps a nav group closed while the current route sits outside it', () => {
        renderSettings([{title: 'Organization'}, aiNavGroup], '/automation/settings/users');

        expect(screen.getByRole('button', {name: 'AI'})).toBeInTheDocument();
        expect(screen.queryByRole('link', {name: 'Providers'})).not.toBeInTheDocument();
    });

    it('opens a closed nav group when its parent row is clicked', async () => {
        renderSettings([{title: 'Organization'}, aiNavGroup], '/automation/settings/users');

        await userEvent.click(screen.getByRole('button', {name: 'AI'}));

        expect(screen.getByRole('link', {name: 'Providers'})).toBeInTheDocument();
    });

    it('marks a collapsed nav group as current so closing it does not lose the you-are-here mark', async () => {
        renderSettings([{title: 'Organization'}, aiNavGroup], '/automation/settings/ai/skills');

        const groupRow = screen.getByRole('button', {name: 'AI'});

        expect(groupRow).not.toHaveClass('bg-accent');

        await userEvent.click(groupRow);

        expect(groupRow).toHaveClass('bg-accent');
    });

    it('drops a nav group whose every entry is hidden by a feature flag', () => {
        renderSettings([
            {title: 'Organization'},
            {href: 'users', title: 'Users'},
            {items: [{href: 'custom-components', title: 'Custom Components'}], title: 'AI'},
        ]);

        expect(screen.queryByText('AI')).not.toBeInTheDocument();
        expect(screen.queryByText('Custom Components')).not.toBeInTheDocument();
        expect(screen.getByText('Users')).toBeInTheDocument();
    });
});
