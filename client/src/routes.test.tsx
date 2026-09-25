import {getRouter, loadEnvironments, loadProjectWorkflowEditor} from '@/routes';
import {AUTHORITIES, DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import Settings, {SettingsNavItemI} from '@/shared/layout/Settings';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {ReactElement, ReactNode} from 'react';
import {MemoryRouter, Route, RouteObject, Routes, useLocation} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    isTenantAdmin: false,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    createBrowserRouter: (routes: RouteObject[]) => ({routes}),
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
}));

vi.mock('@/shared/layout/Header', () => ({
    default: () => null,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({leftSidebarBody}: {leftSidebarBody: ReactNode}) => <div>{leftSidebarBody}</div>,
}));

vi.mock('@/shared/layout/LeftSidebarNav', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/layout/LeftSidebarNav')>()),
    LeftSidebarNav: ({body}: {body: ReactNode}) => <div>{body}</div>,
    LeftSidebarNavItem: ({toLink}: {toLink: string}) => <span data-nav-href={toLink} />,
}));

vi.mock('@/shared/layout/SettingsNavGroup', () => ({
    default: ({items}: {items: {href: string}[]}) =>
        items.map((item) => <span data-nav-href={item.href} key={item.href} />),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/stores/useApplicationInfoStore')>()),
    useApplicationInfoStore: () => true,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/stores/useFeatureFlagsStore')>()),
    useFeatureFlagsStore: () => () => true,
}));

type PrivateRouteElementType = ReactElement<{hasAnyAuthorities?: string[]}>;

type SettingsElementType = ReactElement<{sidebarNavItems: SettingsNavItemI[]}>;

interface SettingsNavEntryI {
    href: string;
    tenantAdminOnly: boolean;
}

interface SettingsRouteI {
    element: SettingsElementType;
    navEntries: SettingsNavEntryI[];
    path: string;
}

const findSettingsRoutes = (routes: RouteObject[]): RouteObject[] =>
    routes.flatMap((route) => [
        ...((route.element as ReactElement | undefined)?.type === Settings ? [route] : []),
        ...(route.children ? findSettingsRoutes(route.children) : []),
    ]);

const findNavItemRoute = (settingsRoute: RouteObject, href: string) => {
    const settingsPathSegment = `/${settingsRoute.path}`;

    const relativePath = href.startsWith('/')
        ? href.slice(href.indexOf(settingsPathSegment) + settingsPathSegment.length).replace(/^\//, '')
        : href;

    return settingsRoute.children?.find((childRoute) =>
        relativePath === '' ? childRoute.index : childRoute.path === relativePath
    );
};

const isTenantAdminOnly = (route: RouteObject) => {
    const guardedRoutes = route.element ? [route] : (route.children ?? []).filter((childRoute) => childRoute.element);

    return (
        guardedRoutes.length > 0 &&
        guardedRoutes.every((guardedRoute) => {
            const authorities = (guardedRoute.element as PrivateRouteElementType).props.hasAnyAuthorities;

            return authorities?.length === 1 && authorities[0] === AUTHORITIES.ADMIN;
        })
    );
};

const getSettingsRoutes = (): SettingsRouteI[] => {
    const router = getRouter(new QueryClient());

    return findSettingsRoutes(router.routes as RouteObject[]).map((settingsRoute) => {
        const element = settingsRoute.element as SettingsElementType;

        const navEntries = element.props.sidebarNavItems
            .flatMap((navItem) => navItem.items ?? [navItem])
            .filter((navItem) => navItem.href !== undefined)
            .map((navItem) => {
                const navItemRoute = findNavItemRoute(settingsRoute, navItem.href!);

                expect(navItemRoute, `route for settings entry ${navItem.href}`).toBeDefined();

                return {href: navItem.href!, tenantAdminOnly: isTenantAdminOnly(navItemRoute!)};
            });

        return {element, navEntries, path: settingsRoute.path!};
    });
};

const findSettingsIndexElements = (routes: RouteObject[], parentPath = ''): Record<string, ReactElement> =>
    Object.fromEntries(
        routes.flatMap((route) => {
            const routePath = route.path ? `${parentPath}/${route.path}`.replace(/\/+/g, '/') : parentPath;

            const indexRoute =
                (route.element as ReactElement | undefined)?.type === Settings
                    ? route.children?.find((childRoute) => childRoute.index)
                    : undefined;

            return [
                ...(indexRoute ? [[routePath, indexRoute.element as ReactElement]] : []),
                ...Object.entries(route.children ? findSettingsIndexElements(route.children, routePath) : {}),
            ];
        })
    );

const LocationPathname = () => <span data-testid="pathname">{useLocation().pathname}</span>;

const resolveSettingsIndexRedirect = (settingsPath: string) => {
    const indexElement = findSettingsIndexElements(getRouter(new QueryClient()).routes as RouteObject[])[settingsPath];

    expect(indexElement, `index route for ${settingsPath}`).toBeDefined();

    const {getByTestId, unmount} = render(
        <MemoryRouter initialEntries={[settingsPath]}>
            <Routes>
                <Route element={indexElement} path={settingsPath} />

                <Route element={<LocationPathname />} path="*" />
            </Routes>
        </MemoryRouter>
    );

    const pathname = getByTestId('pathname').textContent;

    unmount();

    return pathname;
};

const renderNavHrefs = (element: SettingsElementType) => {
    const {container, unmount} = render(<MemoryRouter>{element}</MemoryRouter>);

    const navHrefs = Array.from(container.querySelectorAll('[data-nav-href]')).map((navElement) =>
        navElement.getAttribute('data-nav-href')
    );

    unmount();

    return navHrefs;
};

const findRoutesByPath = (routes: RouteObject[], path: string): RouteObject[] =>
    routes.flatMap((route) => [
        ...(route.path === path ? [route] : []),
        ...(route.children ? findRoutesByPath(route.children, path) : []),
    ]);

const getRouteAuthorities = (path: string) => {
    const router = getRouter(new QueryClient());

    const routes = findRoutesByPath(router.routes as RouteObject[], path);

    return routes.map((route) => (route.element as PrivateRouteElementType).props.hasAnyAuthorities);
};

const getChildRouteAuthorities = (path: string) => {
    const router = getRouter(new QueryClient());

    const routes = findRoutesByPath(router.routes as RouteObject[], path);

    return routes
        .flatMap((route) => route.children ?? [])
        .map((route) => (route.element as PrivateRouteElementType).props.hasAnyAuthorities);
};

describe('loadEnvironmentsIfAuthenticated', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('does nothing when not authenticated', async () => {
        // Arrange
        vi.spyOn(authenticationStore, 'getState').mockReturnValue({authenticated: false} as never);

        const fetchSpy = vi.spyOn(queryClient, 'fetchQuery');

        const setEnvironmentsSpy = vi.fn();
        vi.spyOn(environmentStore, 'getState').mockReturnValue({setEnvironments: setEnvironmentsSpy} as never);

        // Act
        await loadEnvironments(queryClient);

        // Assert
        expect(fetchSpy).not.toHaveBeenCalled();
        expect(setEnvironmentsSpy).not.toHaveBeenCalled();
    });

    it('fetches environments and sets them when authenticated', async () => {
        // Arrange
        vi.spyOn(authenticationStore, 'getState').mockReturnValue({authenticated: true} as never);

        const environments = [
            {id: 1, name: 'Dev'},
            {id: 2, name: 'Prod'},
        ];
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const fetchSpy = vi.spyOn(queryClient, 'fetchQuery').mockResolvedValue(environments as any);

        const setEnvironmentsSpy = vi.fn();
        vi.spyOn(environmentStore, 'getState').mockReturnValue({setEnvironments: setEnvironmentsSpy} as never);

        // Act
        await loadEnvironments(queryClient);

        // Assert
        expect(fetchSpy).toHaveBeenCalledTimes(1);
        expect(setEnvironmentsSpy).toHaveBeenCalledWith(environments);
    });
});

describe('loadProjectWorkflowEditor', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient();
    });

    afterEach(() => {
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});

        vi.restoreAllMocks();
    });

    it('loads the project when Development is selected', async () => {
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});

        const project = {id: 7, name: 'Project'};

        const ensureQueryDataSpy = vi.spyOn(queryClient, 'ensureQueryData').mockResolvedValue(project);

        const result = await loadProjectWorkflowEditor(queryClient, 7);

        expect(ensureQueryDataSpy).toHaveBeenCalledTimes(1);
        expect(result).toBe(project);
    });

    it('redirects to deployments instead of opening the editor outside Development', async () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});

        const ensureQueryDataSpy = vi.spyOn(queryClient, 'ensureQueryData');

        const result = await loadProjectWorkflowEditor(queryClient, 7);

        expect(ensureQueryDataSpy).not.toHaveBeenCalled();
        expect(result).toBeInstanceOf(Response);
        expect((result as Response).headers.get('Location')).toBe('/automation/deployments');
    });
});

describe('settings route authorities', () => {
    it('lets only a tenant admin open the Billing settings page', () => {
        const authorities = getRouteAuthorities('billing');

        expect(authorities.length).toBeGreaterThan(0);
        authorities.forEach((routeAuthorities) => expect(routeAuthorities).toEqual([AUTHORITIES.ADMIN]));
    });

    it('lets only a tenant admin open the MCP Server settings page', () => {
        const authorities = getRouteAuthorities('mcp-server');

        expect(authorities.length).toBeGreaterThan(0);
        authorities.forEach((routeAuthorities) => expect(routeAuthorities).toEqual([AUTHORITIES.ADMIN]));
    });

    it('lets only a tenant admin open the API Connectors settings pages', () => {
        const authorities = getChildRouteAuthorities('api-connectors');

        expect(authorities.length).toBe(8);
        authorities.forEach((routeAuthorities) => expect(routeAuthorities).toEqual([AUTHORITIES.ADMIN]));
    });

    it('lets only a tenant admin open the embedded MCP Servers page', () => {
        const router = getRouter(new QueryClient());

        const embeddedRoutes = findRoutesByPath(router.routes as RouteObject[], 'embedded');

        const routes = findRoutesByPath(embeddedRoutes, 'mcp-servers');

        expect(routes).toHaveLength(1);
        expect((routes[0].element as PrivateRouteElementType).props.hasAnyAuthorities).toEqual([AUTHORITIES.ADMIN]);
    });

    it.each([
        'integrations',
        'integrations/:integrationId/integration-workflows/:integrationWorkflowId',
        'configurations',
        'automation-workflows',
        'automation-workflows/:workflowId/editor',
        'connected-users',
        'app-events',
        'executions',
        'connections',
    ])('lets only a tenant admin open the embedded %s page', (path) => {
        const router = getRouter(new QueryClient());

        const embeddedRoutes = findRoutesByPath(router.routes as RouteObject[], 'embedded');

        const routes = findRoutesByPath(embeddedRoutes, path);

        expect(routes).toHaveLength(1);
        expect((routes[0].element as PrivateRouteElementType).props.hasAnyAuthorities).toEqual([AUTHORITIES.ADMIN]);
    });

    it('lets only a tenant admin open the API Clients page', () => {
        const authorities = getRouteAuthorities('api-clients');

        expect(authorities.length).toBeGreaterThan(0);
        authorities.forEach((routeAuthorities) => expect(routeAuthorities).toEqual([AUTHORITIES.ADMIN]));
    });
});

describe('settings navigation visibility', () => {
    beforeEach(() => {
        hoisted.isTenantAdmin = false;
    });

    it('derives tenant-admin-only entries from the route tree', () => {
        const settingsRoutes = getSettingsRoutes();

        expect(settingsRoutes.map((settingsRoute) => settingsRoute.path)).toEqual(
            expect.arrayContaining(['account', 'settings'])
        );
        expect(
            settingsRoutes.flatMap((settingsRoute) => settingsRoute.navEntries).filter((entry) => entry.tenantAdminOnly)
                .length
        ).toBeGreaterThan(0);
    });

    it('hides from a non-admin every settings entry whose route only a tenant admin may open', () => {
        getSettingsRoutes().forEach(({element, navEntries}) => {
            const navHrefs = renderNavHrefs(element);

            navEntries.forEach(({href, tenantAdminOnly}) => {
                if (tenantAdminOnly) {
                    expect(navHrefs, `tenant-admin-only entry ${href}`).not.toContain(href);
                } else {
                    expect(navHrefs, `entry ${href}`).toContain(href);
                }
            });
        });
    });

    it('shows a tenant admin every settings entry', () => {
        hoisted.isTenantAdmin = true;

        getSettingsRoutes().forEach(({element, navEntries}) => {
            const navHrefs = renderNavHrefs(element);

            navEntries.forEach(({href}) => expect(navHrefs, `entry ${href}`).toContain(href));
        });
    });

    it('sends a tenant admin from the settings index to the default settings page', () => {
        hoisted.isTenantAdmin = true;

        expect(resolveSettingsIndexRedirect('/automation/settings')).toBe('/automation/settings/workspaces');
        expect(resolveSettingsIndexRedirect('/embedded/settings')).toBe('/embedded/settings/signing-keys');
    });

    it('sends a non-admin from the settings index to the first settings entry they can see', () => {
        expect(resolveSettingsIndexRedirect('/automation/settings')).toBe('/automation/settings/workspace-users');
        expect(resolveSettingsIndexRedirect('/embedded/settings')).toBe('/embedded/settings/ai/skills');
    });
});
