import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppLeftSidebar from '../components/AppLeftSidebar';

const hoisted = vi.hoisted(() => ({
    apps: [] as unknown[],
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppsQuery: vi.fn(() => ({data: {apps: hoisted.apps}, error: null, isLoading: false})),
}));

const renderSidebar = (currentId?: string) =>
    render(
        <MemoryRouter>
            <AppLeftSidebar currentId={currentId} />
        </MemoryRouter>
    );

describe('AppLeftSidebar', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.apps = [
            {id: '2', name: 'Solo Invoices'},
            {id: '1', name: 'Airtable Dashboard'},
            {id: '3', name: 'orders walkthrough'},
        ];

        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('lists the apps of the workspace by name, each linking to its page', async () => {
        renderSidebar();

        const links = screen.getAllByRole('link');

        expect(links.map((link) => link.textContent)).toEqual([
            'Airtable Dashboard',
            'orders walkthrough',
            'Solo Invoices',
        ]);
        expect(links[0]).toHaveAttribute('href', '/automation/apps/1');

        const {useAppsQuery} = await import('@/shared/middleware/graphql');

        expect(vi.mocked(useAppsQuery)).toHaveBeenCalledWith({workspaceId: '7'});
    });

    it('marks the open app current', () => {
        renderSidebar('3');

        expect(screen.getByRole('link', {name: 'orders walkthrough'})).toHaveAttribute('aria-current', 'page');
        expect(screen.getByRole('link', {name: 'Solo Invoices'})).not.toHaveAttribute('aria-current');
    });

    it('filters the list by the search text', async () => {
        renderSidebar();

        await userEvent.type(screen.getByPlaceholderText('Search apps...'), 'INVO');

        expect(screen.getAllByRole('link').map((link) => link.textContent)).toEqual(['Solo Invoices']);
    });

    it('says so when no app matches', async () => {
        renderSidebar();

        await userEvent.type(screen.getByPlaceholderText('Search apps...'), 'zzz');

        expect(screen.getByText('No apps found')).toBeInTheDocument();
    });
});
