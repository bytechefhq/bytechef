import {getRouter} from '@/routes';
import {AUTHORITIES} from '@/shared/constants';
import Settings, {SettingsNavItemI} from '@/shared/layout/Settings';
import {render} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {ReactElement, ReactNode} from 'react';
import {MemoryRouter, Route, RouteObject, Routes, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

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
