import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {getRouter, loadEnvironments, loadProjectWorkflowEditor} from '@/routes';
import {AUTHORITIES, DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import EmbeddedEnvironmentAccessGuard from '@/shared/layout/EmbeddedEnvironmentAccessGuard';
import Settings, {SettingsNavItemI} from '@/shared/layout/Settings';
import SettingsLayout from '@/shared/layout/SettingsLayout';
import {getSettingsNavItems} from '@/shared/layout/settingsNavItems';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {ReactElement, ReactNode} from 'react';
import {MemoryRouter, Route, RouteObject, Routes, useLocation} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    embeddedScopes: ['API_KEY_VIEW', 'EMBEDDED_SETTINGS_MANAGE', 'EMBEDDED_VIEW'] as string[],
    isTenantAdmin: false,
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>()),
    useHasEmbeddedScope: (scope: string) => hoisted.embeddedScopes.includes(scope),
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

interface SettingsRouteObjectI {
    fullPath: string;
    route: RouteObject;
}

const isSettingsElement = (element: ReactElement | undefined) =>
    element?.type === Settings || element?.type === SettingsLayout;

const joinRoutePath = (parentPath: string, route: RouteObject) =>
    route.path ? `${parentPath}/${route.path}`.replace(/\/+/g, '/') : parentPath;

const findSettingsRoutes = (routes: RouteObject[], parentPath = ''): SettingsRouteObjectI[] =>
    routes.flatMap((route) => {
        const fullPath = joinRoutePath(parentPath, route);

        return [
            ...(isSettingsElement(route.element as ReactElement | undefined) ? [{fullPath, route}] : []),
            ...(route.children ? findSettingsRoutes(route.children, fullPath) : []),
        ];
    });

const toSettingsElement = ({fullPath, route}: SettingsRouteObjectI): SettingsElementType => {
    const element = route.element as ReactElement;

    if (element.type === SettingsLayout) {
        const platformType = fullPath.startsWith('/embedded/') ? PlatformType.EMBEDDED : PlatformType.AUTOMATION;

        return <Settings sidebarNavItems={getSettingsNavItems(platformType)} />;
    }

    return element as SettingsElementType;
};

const findNavItemRoute = (settingsRoutes: SettingsRouteObjectI[], href: string) => {
    const ownerSettingsRoute = settingsRoutes
        .filter(({fullPath}) => href === fullPath || href.startsWith(`${fullPath}/`))
        .sort(
            (settingsRoute, otherSettingsRoute) => otherSettingsRoute.fullPath.length - settingsRoute.fullPath.length
        )[0];

    if (!ownerSettingsRoute) {
        return undefined;
    }

    const relativePath = href.slice(ownerSettingsRoute.fullPath.length).replace(/^\//, '');

    return ownerSettingsRoute.route.children?.find((childRoute) =>
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

    const settingsRoutes = findSettingsRoutes(router.routes as RouteObject[]);

    return settingsRoutes.map((settingsRoute) => {
        const element = toSettingsElement(settingsRoute);

        const navEntries = element.props.sidebarNavItems
            .flatMap((navItem) => navItem.items ?? [navItem])
            .filter((navItem) => navItem.href !== undefined)
            .map((navItem) => {
                const navItemRoute = findNavItemRoute(settingsRoutes, navItem.href!);

                expect(navItemRoute, `route for settings entry ${navItem.href}`).toBeDefined();

                return {href: navItem.href!, tenantAdminOnly: isTenantAdminOnly(navItemRoute!)};
            });

        return {element, navEntries, path: settingsRoute.route.path!};
    });
};

const findSettingsIndexElements = (routes: RouteObject[], parentPath = ''): Record<string, ReactElement> =>
    Object.fromEntries(
        routes.flatMap((route) => {
            const routePath = joinRoutePath(parentPath, route);

            const indexRoute = isSettingsElement(route.element as ReactElement | undefined)
                ? route.children?.find((childRoute) => childRoute.index)
                : undefined;

            return [
                ...(indexRoute?.element ? [[routePath, indexRoute.element as ReactElement]] : []),
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

        const fetchSpy = vi.spyOn(queryClient, 'query');

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
        const fetchSpy = vi.spyOn(queryClient, 'query').mockResolvedValue(environments as any);

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

        const querySpy = vi.spyOn(queryClient, 'query').mockResolvedValue(project);

        const result = await loadProjectWorkflowEditor(queryClient, 7);

        expect(querySpy).toHaveBeenCalledTimes(1);
        expect(result).toBe(project);
    });

    it('redirects to deployments instead of opening the editor outside Development', async () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});

        const querySpy = vi.spyOn(queryClient, 'query');

        const result = await loadProjectWorkflowEditor(queryClient, 7);

        expect(querySpy).not.toHaveBeenCalled();
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

    it('lets any user open the MCP Server settings page', () => {
        const authorities = getRouteAuthorities('mcp-server');

        expect(authorities.length).toBeGreaterThan(0);
        authorities.forEach((routeAuthorities) =>
            expect(routeAuthorities).toEqual([AUTHORITIES.ADMIN, AUTHORITIES.USER])
        );
    });

    it('lets only a tenant admin open the API Connectors settings pages', () => {
        const authorities = getChildRouteAuthorities('api-connectors');

        expect(authorities.length).toBe(4);
        authorities.forEach((routeAuthorities) => expect(routeAuthorities).toEqual([AUTHORITIES.ADMIN]));
    });

    it.each([
        'integrations',
        'integrations/:integrationId/integration-workflows/:integrationWorkflowId',
        'configurations',
        'automations',
        'automations/:workflowUuid/editor',
        'automation-configurations',
        'app-events',
        'workflow-executions',
        'external-invocations',
        'connections',
        'mcp-servers',
    ])('testEveryEmbeddedRouteAdmitsUsers %s', (path) => {
        const router = getRouter(new QueryClient());

        const embeddedRoutes = findRoutesByPath(router.routes as RouteObject[], 'embedded');

        const routes = findRoutesByPath(embeddedRoutes, path);

        expect(routes).toHaveLength(1);
        expect((routes[0].element as PrivateRouteElementType).props.hasAnyAuthorities).toEqual([
            AUTHORITIES.ADMIN,
            AUTHORITIES.USER,
        ]);
    });

    it.each(['members', 'signing-keys', 'api-keys'])('testEveryEmbeddedRouteAdmitsUsers settings/%s', (path) => {
        const router = getRouter(new QueryClient());

        const embeddedRoutes = findRoutesByPath(router.routes as RouteObject[], 'embedded');

        const routes = findRoutesByPath(embeddedRoutes, path);

        expect(routes).toHaveLength(1);
        expect((routes[0].element as PrivateRouteElementType).props.hasAnyAuthorities).toEqual([
            AUTHORITIES.ADMIN,
            AUTHORITIES.USER,
        ]);
    });

    it('wraps the embedded routes in the environment access guard', () => {
        const router = getRouter(new QueryClient());

        const [embeddedRoute] = findRoutesByPath(router.routes as RouteObject[], 'embedded');

        expect((embeddedRoute.element as ReactElement).type).toBe(EmbeddedEnvironmentAccessGuard);
    });

    it('lets only a tenant admin open the API Clients page', () => {
        const authorities = getRouteAuthorities('api-clients');

        expect(authorities.length).toBeGreaterThan(0);
        authorities.forEach((routeAuthorities) => expect(routeAuthorities).toEqual([AUTHORITIES.ADMIN]));
    });
});

describe('settings navigation visibility', () => {
    beforeEach(() => {
        hoisted.embeddedScopes = ['API_KEY_VIEW', 'EMBEDDED_SETTINGS_MANAGE', 'EMBEDDED_VIEW'];
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
        expect(resolveSettingsIndexRedirect('/embedded/settings')).toBe('/embedded/settings/members');
    });

    it('sends an embedded member without signing key access to the first settings entry they can see', () => {
        hoisted.embeddedScopes = ['EMBEDDED_SETTINGS_MANAGE'];

        expect(resolveSettingsIndexRedirect('/embedded/settings')).toBe('/embedded/settings/signing-keys');

        hoisted.embeddedScopes = ['API_KEY_VIEW'];

        expect(resolveSettingsIndexRedirect('/embedded/settings')).toBe('/embedded/settings/api-keys');

        hoisted.embeddedScopes = [];

        expect(resolveSettingsIndexRedirect('/embedded/settings')).toBe('/settings/mcp-server');
    });
});
