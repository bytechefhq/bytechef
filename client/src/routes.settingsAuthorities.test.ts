import {getRouter} from '@/routes';
import {AUTHORITIES} from '@/shared/constants';
import {QueryClient} from '@tanstack/react-query';
import {ReactElement} from 'react';
import {RouteObject} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    createBrowserRouter: (routes: RouteObject[]) => ({routes}),
}));

type PrivateRouteElementType = ReactElement<{hasAnyAuthorities?: string[]}>;

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
