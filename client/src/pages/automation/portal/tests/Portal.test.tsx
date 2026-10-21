import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {
    act,
    fireEvent,
    mockScrollIntoView,
    render,
    screen,
    userEvent,
    waitFor,
    windowResizeObserver,
    within,
} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Portal from '../Portal';

interface PortalAppInstanceFixtureI {
    id: string;
    lastUsedDate: string | null;
    name: string;
    url: string;
    useCount: number;
    workspaceId: string;
    workspaceName: string;
}

interface PortalAppFixtureI {
    categoryId: string | null;
    categoryName: string | null;
    description: string | null;
    favorite: boolean;
    hasIcon: boolean;
    id: string;
    instances: PortalAppInstanceFixtureI[];
    lastUsedDate: string | null;
    name: string;
}

const hoisted = vi.hoisted(() => ({
    callOrder: [] as string[],
    portalAppsByEnvironment: {} as Record<string, unknown[]>,
    portalAppsRequests: [] as Array<{environmentId: string}>,
    recordAppLaunch: vi.fn(),
    setAppFavorite: vi.fn(),
}));

vi.mock('@/shared/middleware/graphqlFetcher', () => ({
    fetcher: (document: {toString(): string}, variables: Record<string, unknown>) => (): Promise<unknown> => {
        const operation = document.toString();

        if (operation.includes('query portalApps')) {
            hoisted.portalAppsRequests.push(variables as {environmentId: string});

            return Promise.resolve({
                portalApps: hoisted.portalAppsByEnvironment[variables.environmentId as string] ?? [],
            });
        }

        if (operation.includes('mutation setAppFavorite')) {
            return hoisted.setAppFavorite(variables);
        }

        if (operation.includes('mutation recordAppLaunch')) {
            hoisted.callOrder.push('recordAppLaunch');

            return hoisted.recordAppLaunch(variables);
        }

        return Promise.reject(new Error(`unexpected operation ${operation}`));
    },
}));

const instance = (overrides: Partial<PortalAppInstanceFixtureI> = {}): PortalAppInstanceFixtureI => ({
    id: '101',
    lastUsedDate: null,
    name: 'crm-prod',
    url: 'https://crm.example.com',
    useCount: 0,
    workspaceId: '7',
    workspaceName: 'Sales',
    ...overrides,
});

const portalApp = (overrides: Partial<PortalAppFixtureI> = {}): PortalAppFixtureI => ({
    categoryId: null,
    categoryName: null,
    description: null,
    favorite: false,
    hasIcon: false,
    id: '1',
    instances: [instance()],
    lastUsedDate: null,
    name: 'CRM Dashboard',
    ...overrides,
});

const renderPortal = () =>
    render(
        <MemoryRouter initialEntries={['/automation/portal']}>
            <TooltipProvider>
                <Routes>
                    <Route element={<Portal />} path="/automation/portal" />

                    <Route element={<div>Automation home page</div>} path="/automation" />
                </Routes>
            </TooltipProvider>
        </MemoryRouter>
    );

const getSection = (title: string) => screen.getByRole('region', {name: title});

describe('Portal', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockScrollIntoView();

        if (!Element.prototype.hasPointerCapture) {
            Element.prototype.hasPointerCapture = vi.fn(() => false);
            Element.prototype.setPointerCapture = vi.fn();
            Element.prototype.releasePointerCapture = vi.fn();
        }

        vi.clearAllMocks();

        hoisted.callOrder = [];
        hoisted.portalAppsByEnvironment = {};
        hoisted.portalAppsRequests = [];
        hoisted.recordAppLaunch.mockResolvedValue({recordAppLaunch: true});
        hoisted.setAppFavorite.mockResolvedValue({setAppFavorite: true});

        vi.mocked(window.open).mockImplementation(() => {
            hoisted.callOrder.push('open');

            return null;
        });

        environmentStore.setState({currentEnvironmentId: 2});
    });

    it('goes back to Automation from the back arrow', async () => {
        hoisted.portalAppsByEnvironment['2'] = [];

        renderPortal();

        await userEvent.setup().click(screen.getByRole('button', {name: 'Back'}));

        expect(await screen.findByText('Automation home page')).toBeInTheDocument();
    });

    it('fills the page without an empty left sidebar', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp()];

        renderPortal();

        expect(await screen.findByText('CRM Dashboard')).toBeInTheDocument();
        expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
    });

    it('explains how apps get here when the member has none', async () => {
        hoisted.portalAppsByEnvironment['2'] = [];

        renderPortal();

        expect(await screen.findByText('No Apps Yet')).toBeInTheDocument();
        expect(screen.getByText(/once an administrator marks them discoverable/)).toBeInTheDocument();
    });

    it('loads the apps of the current environment and refetches when the environment changes', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp({id: '1', name: 'Staging Tool'})];
        hoisted.portalAppsByEnvironment['3'] = [portalApp({id: '2', name: 'Production Tool'})];

        renderPortal();

        expect(await screen.findByText('Staging Tool')).toBeInTheDocument();

        act(() => {
            environmentStore.setState({currentEnvironmentId: 3});
        });

        expect(await screen.findByText('Production Tool')).toBeInTheDocument();
        expect(screen.queryByText('Staging Tool')).not.toBeInTheDocument();
        expect(hoisted.portalAppsRequests).toEqual([{environmentId: '2'}, {environmentId: '3'}]);
    });

    it('filters by name and by description, ignoring case', async () => {
        hoisted.portalAppsByEnvironment['2'] = [
            portalApp({description: 'Track invoices', id: '1', name: 'Billing'}),
            portalApp({description: 'Customer records', id: '2', name: 'CRM Dashboard'}),
        ];

        renderPortal();

        const allApps = await screen.findByRole('region', {name: 'All apps'});

        await userEvent.type(screen.getByPlaceholderText('Search apps...'), 'crm');

        expect(within(allApps).getByText('CRM Dashboard')).toBeInTheDocument();
        expect(within(allApps).queryByText('Billing')).not.toBeInTheDocument();

        await userEvent.clear(screen.getByPlaceholderText('Search apps...'));
        await userEvent.type(screen.getByPlaceholderText('Search apps...'), 'INVOICES');

        expect(within(getSection('All apps')).getByText('Billing')).toBeInTheDocument();
        expect(within(getSection('All apps')).queryByText('CRM Dashboard')).not.toBeInTheDocument();
    });

    it('says so when nothing matches the search, and clears it', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp()];

        renderPortal();

        await screen.findByText('CRM Dashboard');

        await userEvent.type(screen.getByPlaceholderText('Search apps...'), 'nothing like it');

        expect(screen.getByText('No Matching Apps')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Clear filters'}));

        expect(screen.getByText('CRM Dashboard')).toBeInTheDocument();
    });

    it('lists favorites and recently used apps, newest first, above every app in alphabetical order', async () => {
        hoisted.portalAppsByEnvironment['2'] = [
            portalApp({id: '1', lastUsedDate: '2026-11-10T10:00:00Z', name: 'Zeta'}),
            portalApp({favorite: true, id: '2', name: 'Alpha'}),
            portalApp({id: '3', lastUsedDate: '2026-11-15T10:00:00Z', name: 'Mid'}),
        ];

        renderPortal();

        await screen.findByRole('region', {name: 'All apps'});

        const favoriteNames = within(getSection('Favorites'))
            .getAllByRole('heading', {level: 3})
            .map((heading) => heading.textContent);
        const recentNames = within(getSection('Recently used'))
            .getAllByRole('heading', {level: 3})
            .map((heading) => heading.textContent);
        const allNames = within(getSection('All apps'))
            .getAllByRole('heading', {level: 3})
            .map((heading) => heading.textContent);

        expect(favoriteNames).toEqual(['Alpha']);
        expect(recentNames).toEqual(['Mid', 'Zeta']);
        expect(allNames).toEqual(['Alpha', 'Mid', 'Zeta']);
    });

    it('shows no Favorites or Recently used section when there is nothing to put in them', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp()];

        renderPortal();

        await screen.findByRole('region', {name: 'All apps'});

        expect(screen.queryByRole('region', {name: 'Favorites'})).not.toBeInTheDocument();
        expect(screen.queryByRole('region', {name: 'Recently used'})).not.toBeInTheDocument();
    });

    it('launches the only instance directly: opens it in a new tab before recording the launch', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp()];

        renderPortal();

        await userEvent.click(await screen.findByRole('button', {name: 'Open CRM Dashboard'}));

        expect(window.open).toHaveBeenCalledWith('https://crm.example.com/', '_blank', 'noopener,noreferrer');

        await waitFor(() => {
            expect(hoisted.recordAppLaunch).toHaveBeenCalledWith({appInstanceId: '101'});
        });

        expect(hoisted.callOrder).toEqual(['open', 'recordAppLaunch']);
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('opens the tab in the click handler itself, not after the mutation', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp()];
        hoisted.recordAppLaunch.mockReturnValue(new Promise(() => undefined));

        renderPortal();

        const openButton = await screen.findByRole('button', {name: 'Open CRM Dashboard'});

        fireEvent.click(openButton);

        expect(window.open).toHaveBeenCalledTimes(1);
    });

    it('still launches when recording the launch fails', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp()];
        hoisted.recordAppLaunch.mockRejectedValue(new Error('boom'));

        renderPortal();

        await userEvent.click(await screen.findByRole('button', {name: 'Open CRM Dashboard'}));

        await waitFor(() => {
            expect(hoisted.recordAppLaunch).toHaveBeenCalled();
        });

        expect(window.open).toHaveBeenCalledWith('https://crm.example.com/', '_blank', 'noopener,noreferrer');
        expect(screen.getByRole('button', {name: 'Open CRM Dashboard'})).toBeEnabled();
    });

    it('opens the instance selector for several instances and launches the chosen one', async () => {
        hoisted.portalAppsByEnvironment['2'] = [
            portalApp({
                instances: [
                    instance({id: '101', name: 'crm-us', url: 'https://us.crm.example.com', workspaceName: 'Sales US'}),
                    instance({id: '102', name: 'crm-eu', url: 'https://eu.crm.example.com', workspaceName: 'Sales EU'}),
                ],
            }),
        ];

        renderPortal();

        await userEvent.click(await screen.findByRole('button', {name: 'Open CRM Dashboard'}));

        expect(window.open).not.toHaveBeenCalled();

        const dialog = await screen.findByRole('dialog');

        expect(within(dialog).getByText('Sales US')).toBeInTheDocument();
        expect(within(dialog).getByText('Sales EU')).toBeInTheDocument();

        await userEvent.click(within(dialog).getByRole('button', {name: /crm-eu/}));

        expect(window.open).toHaveBeenCalledWith('https://eu.crm.example.com/', '_blank', 'noopener,noreferrer');

        await waitFor(() => {
            expect(hoisted.recordAppLaunch).toHaveBeenCalledWith({appInstanceId: '102'});
        });
    });

    it('never opens an instance whose address is not http or https', async () => {
        hoisted.portalAppsByEnvironment['2'] = [
            portalApp({instances: [instance({url: 'javascript:alert(1)'})]}),
            portalApp({
                id: '2',
                instances: [
                    instance({id: '201', name: 'bad', url: 'data:text/html,hi'}),
                    instance({id: '202', name: 'good', url: 'http://good.example.com'}),
                ],
                name: 'Other',
            }),
        ];

        renderPortal();

        const singleOpenButton = await screen.findByRole('button', {name: 'Open CRM Dashboard'});

        expect(singleOpenButton).toBeDisabled();
        expect(screen.getByText('This app has no address that can be opened.')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Open Other'}));

        const dialog = await screen.findByRole('dialog');

        expect(within(dialog).getByRole('button', {name: /bad/})).toBeDisabled();
        expect(within(dialog).getByText('No address that can be opened.')).toBeInTheDocument();

        await userEvent.click(within(dialog).getByRole('button', {name: /good/}));

        expect(window.open).toHaveBeenCalledTimes(1);
        expect(window.open).toHaveBeenCalledWith('http://good.example.com/', '_blank', 'noopener,noreferrer');
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });

    it('marks a favorite at once and keeps it when the server agrees', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp()];
        hoisted.setAppFavorite.mockImplementation(() => {
            hoisted.portalAppsByEnvironment['2'] = [portalApp({favorite: true})];

            return Promise.resolve({setAppFavorite: true});
        });

        renderPortal();

        await userEvent.click(await screen.findByRole('button', {name: 'Add CRM Dashboard to favorites'}));

        expect(await screen.findByRole('region', {name: 'Favorites'})).toBeInTheDocument();
        expect(hoisted.setAppFavorite).toHaveBeenCalledWith({appId: '1', favorite: true});

        await waitFor(() => {
            expect(hoisted.portalAppsRequests.length).toBeGreaterThan(1);
        });

        expect(screen.getAllByRole('button', {name: 'Remove CRM Dashboard from favorites'}).length).toBe(2);
    });

    it('shows the favorite before the server answers and rolls it back when the server refuses', async () => {
        let rejectFavorite: (error: Error) => void = () => undefined;

        hoisted.portalAppsByEnvironment['2'] = [portalApp()];
        hoisted.setAppFavorite.mockReturnValue(
            new Promise((_resolve, reject) => {
                rejectFavorite = reject;
            })
        );

        renderPortal();

        await userEvent.click(await screen.findByRole('button', {name: 'Add CRM Dashboard to favorites'}));

        expect(await screen.findByRole('region', {name: 'Favorites'})).toBeInTheDocument();

        act(() => {
            rejectFavorite(new Error('refused'));
        });

        await waitFor(() => {
            expect(screen.queryByRole('region', {name: 'Favorites'})).not.toBeInTheDocument();
        });

        expect(screen.getByRole('button', {name: 'Add CRM Dashboard to favorites'})).toBeInTheDocument();
    });

    it('loads an icon from the portal endpoint and falls back to a glyph when it fails', async () => {
        hoisted.portalAppsByEnvironment['2'] = [
            portalApp({hasIcon: true, id: '42', name: 'With Icon'}),
            portalApp({hasIcon: false, id: '43', name: 'Without Icon'}),
        ];

        renderPortal();

        const allApps = await screen.findByRole('region', {name: 'All apps'});

        const icon = within(allApps).getByTestId('portal-app-icon-42');

        expect(icon.tagName).toBe('IMG');
        expect(icon).toHaveAttribute('src', '/api/automation/internal/portal/apps/42/icon');
        expect(within(allApps).getByTestId('portal-app-icon-43').tagName).not.toBe('IMG');

        fireEvent.error(icon);

        expect(within(allApps).getByTestId('portal-app-icon-42').tagName).not.toBe('IMG');
    });

    it('renders server-supplied text as plain text', async () => {
        hoisted.portalAppsByEnvironment['2'] = [
            portalApp({description: '<img src=x onerror=alert(1)>', name: '<b>Bold</b>'}),
        ];

        renderPortal();

        expect(await screen.findByText('<b>Bold</b>')).toBeInTheDocument();
        expect(screen.getByText('<img src=x onerror=alert(1)>')).toBeInTheDocument();
        expect(document.querySelector('b')).toBeNull();
    });

    it('opens the normalised address that was validated, not the raw string', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp({instances: [instance({url: 'HTTPS://CRM.Example.com'})]})];

        renderPortal();

        await userEvent.click(await screen.findByRole('button', {name: 'Open CRM Dashboard'}));

        expect(window.open).toHaveBeenCalledWith('https://crm.example.com/', '_blank', 'noopener,noreferrer');
    });

    it('shows the category name on the card', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp({categoryId: '9', categoryName: 'Finance'})];

        renderPortal();

        const card = await screen.findByTestId('portal-app-card-1');

        expect(within(card).getByText('Finance')).toBeInTheDocument();
    });

    it('filters by the categories present on the cards and combines the filter with search', async () => {
        hoisted.portalAppsByEnvironment['2'] = [
            portalApp({categoryId: '9', categoryName: 'Finance', description: 'Invoices', id: '1', name: 'Billing'}),
            portalApp({categoryId: '9', categoryName: 'Finance', description: 'Ledger', id: '2', name: 'Books'}),
            portalApp({categoryId: '4', categoryName: 'Sales', id: '3', name: 'CRM Dashboard'}),
        ];

        renderPortal();

        await screen.findByRole('region', {name: 'All apps'});

        await userEvent.click(screen.getByRole('combobox', {name: 'Category'}));

        const options = (await screen.findAllByRole('option')).map((option) => option.textContent);

        expect(options).toEqual(['All categories', 'Finance', 'Sales']);

        await userEvent.click(screen.getByRole('option', {name: 'Finance'}));

        const allNames = () =>
            within(getSection('All apps'))
                .getAllByRole('heading', {level: 3})
                .map((heading) => heading.textContent);

        expect(allNames()).toEqual(['Billing', 'Books']);

        await userEvent.type(screen.getByPlaceholderText('Search apps...'), 'ledger');

        expect(allNames()).toEqual(['Books']);

        await userEvent.clear(screen.getByPlaceholderText('Search apps...'));
        await userEvent.type(screen.getByPlaceholderText('Search apps...'), 'crm');

        expect(screen.getByText('No Matching Apps')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Clear filters'}));

        expect(allNames()).toEqual(['Billing', 'Books', 'CRM Dashboard']);
        expect(screen.getByRole('combobox', {name: 'Category'})).toHaveTextContent('All categories');
    });

    it('offers Uncategorized only when some card has no category', async () => {
        hoisted.portalAppsByEnvironment['2'] = [
            portalApp({categoryId: '9', categoryName: 'Finance', id: '1', name: 'Billing'}),
            portalApp({id: '2', name: 'Loose Tool'}),
        ];

        renderPortal();

        await screen.findByRole('region', {name: 'All apps'});

        await userEvent.click(screen.getByRole('combobox', {name: 'Category'}));

        expect((await screen.findAllByRole('option')).map((option) => option.textContent)).toEqual([
            'All categories',
            'Finance',
            'Uncategorized',
        ]);

        await userEvent.click(screen.getByRole('option', {name: 'Uncategorized'}));

        expect(
            within(getSection('All apps'))
                .getAllByRole('heading', {level: 3})
                .map((heading) => heading.textContent)
        ).toEqual(['Loose Tool']);
    });

    it('offers no Uncategorized choice when every card has a category, and no filter when none has one', async () => {
        hoisted.portalAppsByEnvironment['2'] = [portalApp({categoryId: '9', categoryName: 'Finance'})];
        hoisted.portalAppsByEnvironment['3'] = [portalApp({id: '5', name: 'Plain'})];

        renderPortal();

        await screen.findByRole('region', {name: 'All apps'});

        await userEvent.click(screen.getByRole('combobox', {name: 'Category'}));

        expect(screen.queryByRole('option', {name: 'Uncategorized'})).not.toBeInTheDocument();

        await userEvent.keyboard('{Escape}');

        act(() => {
            environmentStore.setState({currentEnvironmentId: 3});
        });

        expect(await screen.findByText('Plain')).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Category'})).not.toBeInTheDocument();
    });
});
